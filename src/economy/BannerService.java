package economy;

import database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Club or team banners sponsor a team's scoreboard theme for a week. The
 * team (a member of the named role, or staff) must approve; the price is only
 * reserved until then, and unanswered requests are refunded after 48 hours.
 */
public final class BannerService {

    public static final int PRICE = 45;
    public static final long LIFETIME_MS = 7L * 24 * 60 * 60 * 1000;
    public static final long APPROVAL_WINDOW_MS = 48L * 60 * 60 * 1000;

    private static final BannerService INSTANCE = new BannerService();

    private BannerService() { }

    public static BannerService getInstance() {
        return INSTANCE;
    }

    /** A request row. */
    public static final class Request {
        public final long id;
        public final long user;
        public final long roleId;
        public final String team;
        public final String status;

        Request(long id, long user, long roleId, String team, String status) {
            this.id = id;
            this.user = user;
            this.roleId = roleId;
            this.team = team;
            this.status = status;
        }
    }

    /** @return the request ID, or -1 if the player already has a pending one */
    public long request(long user, long roleId, String team) throws SQLException, InsufficientFundsException {
        try {
            return Database.inTransaction(c -> {
                EconomyService.lockWallet(c, user);
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT 1 FROM banner_requests WHERE user = ? AND status = 'PENDING'")) {
                    ps.setLong(1, user);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return -1L;
                    }
                }
                long id;
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO banner_requests (user, role_id, team_name, "
                        + "reservation_id, status, created_at) VALUES (?, ?, ?, '', 'PENDING', ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, user);
                    ps.setLong(2, roleId);
                    ps.setString(3, team);
                    ps.setLong(4, System.currentTimeMillis());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        id = keys.getLong(1);
                    }
                }
                String reservation = "banner:" + id;
                EconomyService.reserveIn(c, user, PRICE, "banner", Long.toString(id), reservation);
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE banner_requests SET reservation_id = ? WHERE id = ?")) {
                    ps.setString(1, reservation);
                    ps.setLong(2, id);
                    ps.executeUpdate();
                }
                return id;
            });
        } catch (EconomyService.InsufficientFundsRuntime e) {
            throw new InsufficientFundsException(0, PRICE);
        }
    }

    public Request get(long id) throws SQLException {
        return Database.inTransaction(c -> load(c, id, false));
    }

    /** Approves: the reserved price is spent and the banner is delivered for a week. */
    public boolean approve(long id) throws SQLException {
        return Database.inTransaction(c -> {
            Request r = load(c, id, true);
            if (r == null || !r.status.equals("PENDING")) return false;
            EconomyService.settleReservationIn(c, "banner:" + id, -PRICE, "shop:banner");
            long now = System.currentTimeMillis();
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO inventory (user, item_code, purchase_id, "
                    + "expires_at, created_at) VALUES (?, 'banner-club', ?, ?, ?)")) {
                ps.setLong(1, r.user);
                ps.setString(2, "banner:" + id);
                ps.setLong(3, now + LIFETIME_MS);
                ps.setLong(4, now);
                ps.executeUpdate();
            }
            setStatus(c, id, "APPROVED");
            return true;
        });
    }

    /** Declines or withdraws a pending request and releases the reservation. */
    public boolean decline(long id) throws SQLException {
        return Database.inTransaction(c -> {
            Request r = load(c, id, true);
            if (r == null || !r.status.equals("PENDING")) return false;
            EconomyService.refundReservationIn(c, "banner:" + id);
            setStatus(c, id, "DECLINED");
            return true;
        });
    }

    /** Refunds requests the team never answered. */
    public int expireUnanswered() throws SQLException {
        List<Long> ids = Database.inTransaction(c -> {
            List<Long> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id FROM banner_requests WHERE status = 'PENDING' AND created_at < ?")) {
                ps.setLong(1, System.currentTimeMillis() - APPROVAL_WINDOW_MS);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(rs.getLong(1));
                }
            }
            return list;
        });
        int n = 0;
        for (long id : ids) if (decline(id)) n++;
        return n;
    }

    /** @return the team named on the player's newest unexpired banner, or null */
    public static String activeTeam(Connection c, long user) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT b.team_name FROM banner_requests b JOIN inventory i "
                + "ON i.purchase_id = CONCAT('banner:', b.id) WHERE b.user = ? AND b.status = 'APPROVED' "
                + "AND i.expires_at > ? ORDER BY b.id DESC LIMIT 1")) {
            ps.setLong(1, user);
            ps.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static Request load(Connection c, long id, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id, user, role_id, team_name, status FROM "
                + "banner_requests WHERE id = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Request(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getString(4),
                        rs.getString(5)) : null;
            }
        }
    }

    private static void setStatus(Connection c, long id, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE banner_requests SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }
}

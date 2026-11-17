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
 * Player-funded bounties. The poster's reward is reserved at posting, paid to
 * the worker when the poster accepts, and returned if the bounty is cancelled
 * or expires unclaimed. Reserved coins still count toward decay.
 */
public final class BountyService {

    public static final int MAX_REWARD = 100;
    public static final int MAX_OPEN_PER_POSTER = 3;
    public static final long LIFETIME_MS = 7L * 24 * 60 * 60 * 1000;

    private static final BountyService INSTANCE = new BountyService();

    private BountyService() { }

    public static BountyService getInstance() {
        return INSTANCE;
    }

    /** A bounty row. */
    public static final class Bounty {
        public final long id;
        public final long poster;
        public final String description;
        public final int reward;
        public final String status;
        public final long claimer;
        public final long expiresAt;

        Bounty(long id, long poster, String description, int reward, String status, long claimer, long expiresAt) {
            this.id = id;
            this.poster = poster;
            this.description = description;
            this.reward = reward;
            this.status = status;
            this.claimer = claimer;
            this.expiresAt = expiresAt;
        }
    }

    /**
     * @return the new bounty ID, or -1 if the poster already has the maximum open
     */
    public long post(long poster, int reward, String description) throws SQLException, InsufficientFundsException {
        if (reward < 1 || reward > MAX_REWARD) throw new IllegalArgumentException("Reward out of range");
        try {
            return Database.inTransaction(c -> {
                EconomyService.lockWallet(c, poster);
                try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM bounties WHERE poster = ? "
                        + "AND status IN ('OPEN', 'CLAIMED')")) {
                    ps.setLong(1, poster);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) >= MAX_OPEN_PER_POSTER) return -1L;
                    }
                }
                long now = System.currentTimeMillis();
                long id;
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO bounties (poster, description, reward, "
                        + "reservation_id, status, created_at, expires_at) VALUES (?, ?, ?, '', 'OPEN', ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, poster);
                    ps.setString(2, description);
                    ps.setInt(3, reward);
                    ps.setLong(4, now);
                    ps.setLong(5, now + LIFETIME_MS);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        id = keys.getLong(1);
                    }
                }
                String reservation = "bounty:" + id;
                EconomyService.reserveIn(c, poster, reward, "bounty", Long.toString(id), reservation);
                try (PreparedStatement ps = c.prepareStatement("UPDATE bounties SET reservation_id = ? WHERE id = ?")) {
                    ps.setString(1, reservation);
                    ps.setLong(2, id);
                    ps.executeUpdate();
                }
                return id;
            });
        } catch (EconomyService.InsufficientFundsRuntime e) {
            throw new InsufficientFundsException(0, reward);
        }
    }

    public List<Bounty> open() throws SQLException {
        return Database.inTransaction(c -> {
            List<Bounty> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT id, poster, description, reward, status, "
                    + "COALESCE(claimer, 0), expires_at FROM bounties WHERE status IN ('OPEN', 'CLAIMED') "
                    + "ORDER BY id DESC LIMIT 15");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Bounty(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getInt(4),
                            rs.getString(5), rs.getLong(6), rs.getLong(7)));
                }
            }
            return list;
        });
    }

    /** @return true if the bounty was open and is now claimed by this worker */
    public boolean claim(long id, long worker) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE bounties SET status = 'CLAIMED', claimer = ? "
                    + "WHERE id = ? AND status = 'OPEN' AND poster <> ? AND expires_at > ?")) {
                ps.setLong(1, worker);
                ps.setLong(2, id);
                ps.setLong(3, worker);
                ps.setLong(4, System.currentTimeMillis());
                return ps.executeUpdate() > 0;
            }
        });
    }

    /**
     * Poster accepts the finished work: reward moves to the worker.
     *
     * @return the worker paid, or 0 if the bounty is not a claimed one of theirs
     */
    public long accept(long id, long poster) throws SQLException {
        return Database.inTransaction(c -> {
            Row r = lock(c, id);
            if (r == null || r.poster != poster || !r.status.equals("CLAIMED")) return 0L;
            EconomyService.settleReservationIn(c, r.reservation, -r.reward, "bounty");
            EconomyService.creditIn(c, r.claimer, r.reward, "bounty:paid", Long.toString(id), "bounty-pay:" + id);
            setStatus(c, id, "PAID");
            return r.claimer;
        });
    }

    /** Poster withdraws an unclaimed bounty and gets the reservation back. */
    public boolean cancel(long id, long poster) throws SQLException {
        return Database.inTransaction(c -> {
            Row r = lock(c, id);
            if (r == null || r.poster != poster || !r.status.equals("OPEN")) return false;
            EconomyService.refundReservationIn(c, r.reservation);
            setStatus(c, id, "CANCELLED");
            return true;
        });
    }

    /** Returns every overdue, unpaid bounty's coins to its poster. */
    public int expireOverdue() throws SQLException {
        List<Long> ids = Database.inTransaction(c -> {
            List<Long> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT id FROM bounties WHERE status IN "
                    + "('OPEN', 'CLAIMED') AND expires_at < ?")) {
                ps.setLong(1, System.currentTimeMillis());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(rs.getLong(1));
                }
            }
            return list;
        });
        int n = 0;
        for (long id : ids) {
            n += Database.inTransaction(c -> {
                Row r = lock(c, id);
                if (r == null || !(r.status.equals("OPEN") || r.status.equals("CLAIMED"))) return 0;
                EconomyService.refundReservationIn(c, r.reservation);
                setStatus(c, id, "EXPIRED");
                return 1;
            });
        }
        return n;
    }

    private static final class Row {
        long poster;
        long claimer;
        int reward;
        String status;
        String reservation;
    }

    private static Row lock(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT poster, COALESCE(claimer, 0), reward, status, "
                + "reservation_id FROM bounties WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Row r = new Row();
                r.poster = rs.getLong(1);
                r.claimer = rs.getLong(2);
                r.reward = rs.getInt(3);
                r.status = rs.getString(4);
                r.reservation = rs.getString(5);
                return r;
            }
        }
    }

    private static void setStatus(Connection c, long id, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE bounties SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }
}

package economy;

import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Purchases are recorded as pending, paid for, and only completed once Discord
 * confirms the role grant. A failed grant refunds the buyer.
 */
public final class ShopService {

    public static final String PENDING = "PENDING";
    public static final String GRANTED = "GRANTED";
    public static final String FAILED = "FAILED";

    private static final long DAY_MS = 86400000L;
    private static final ShopService INSTANCE = new ShopService();

    private ShopService() { }

    public static ShopService getInstance() {
        return INSTANCE;
    }

    /** A purchase waiting on its Discord role grant. */
    public static final class Pending {
        public final String purchaseId;
        public final long user;
        public final long roleId;

        Pending(String purchaseId, long user, long roleId) {
            this.purchaseId = purchaseId;
            this.user = user;
            this.roleId = roleId;
        }
    }

    /**
     * Charges the buyer and records a pending purchase in one transaction.
     */
    public void begin(String purchaseId, long user, String listing, int cost, long roleId, int durationDays)
            throws SQLException, InsufficientFundsException {
        try {
            Database.inTransaction(c -> {
                EconomyService.debitIn(c, user, cost, "shop", purchaseId, purchaseId);
                long now = System.currentTimeMillis();
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO shop_purchases (purchase_id, user, "
                        + "listing, cost, role_id, duration_days, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, purchaseId);
                    ps.setLong(2, user);
                    ps.setString(3, listing);
                    ps.setInt(4, cost);
                    ps.setLong(5, roleId);
                    ps.setInt(6, durationDays);
                    ps.setString(7, PENDING);
                    ps.setLong(8, now);
                    ps.setLong(9, now);
                    ps.executeUpdate();
                }
                return null;
            });
        } catch (EconomyService.InsufficientFundsRuntime e) {
            throw new InsufficientFundsException(0, cost);
        }
    }

    /** Cost of switching colour while keeping the remaining rental time. */
    public static final int SWAP_FEE = 5;

    /**
     * Charges the swap fee and records a pending swap. Needs an unexpired rental.
     *
     * @return the user's current role ID being replaced, or -1 if they have no active rental
     */
    public long beginSwap(String purchaseId, long user, String colour, long newRoleId)
            throws SQLException, InsufficientFundsException {
        try {
            return Database.inTransaction(c -> {
                EconomyService.lockWallet(c, user);
                long oldRole;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT COALESCE(role_id, 0), role_expiry FROM economy WHERE user = ?")) {
                    ps.setLong(1, user);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        oldRole = rs.getLong(1);
                        if (oldRole == 0 || rs.getLong(2) <= System.currentTimeMillis()) return -1L;
                    }
                }
                if (oldRole == newRoleId) return -2L;
                EconomyService.debitIn(c, user, SWAP_FEE, "shop:swap", purchaseId, purchaseId);
                long now = System.currentTimeMillis();
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO shop_purchases (purchase_id, user, "
                        + "listing, cost, role_id, duration_days, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?)")) {
                    ps.setString(1, purchaseId);
                    ps.setLong(2, user);
                    ps.setString(3, "swap:" + colour);
                    ps.setInt(4, SWAP_FEE);
                    ps.setLong(5, newRoleId);
                    ps.setString(6, PENDING);
                    ps.setLong(7, now);
                    ps.setLong(8, now);
                    ps.executeUpdate();
                }
                return oldRole;
            });
        } catch (EconomyService.InsufficientFundsRuntime e) {
            throw new InsufficientFundsException(0, SWAP_FEE);
        }
    }

    /**
     * Marks the purchase granted and starts the rental clock. Renewing the
     * same colour extends the current expiry instead of resetting it.
     *
     * @return false if the purchase was not pending
     */
    public boolean complete(String purchaseId, String colourName) throws SQLException {
        return Database.inTransaction(c -> {
            long user;
            long roleId;
            int days;
            try (PreparedStatement ps = c.prepareStatement("SELECT user, role_id, duration_days FROM shop_purchases "
                    + "WHERE purchase_id = ? AND status = ? FOR UPDATE")) {
                ps.setString(1, purchaseId);
                ps.setString(2, PENDING);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    user = rs.getLong(1);
                    roleId = rs.getLong(2);
                    days = rs.getInt(3);
                }
            }
            EconomyService.lockWallet(c, user);
            if (days == 0) { // colour swap: the rental time carries over unchanged
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE economy SET role_colour = ?, role_id = ? WHERE user = ?")) {
                    ps.setString(1, colourName);
                    ps.setLong(2, roleId);
                    ps.setLong(3, user);
                    ps.executeUpdate();
                }
                setStatus(c, purchaseId, GRANTED);
                return true;
            }
            long now = System.currentTimeMillis();
            long base = now;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT role_expiry, role_id FROM economy WHERE user = ?")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getLong(2) == roleId && rs.getLong(1) > now) base = rs.getLong(1);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE economy SET role_expiry = ?, role_colour = ?, role_id = ? WHERE user = ?")) {
                ps.setLong(1, base + days * DAY_MS);
                ps.setString(2, colourName);
                ps.setLong(3, roleId);
                ps.setLong(4, user);
                ps.executeUpdate();
            }
            setStatus(c, purchaseId, GRANTED);
            return true;
        });
    }

    /**
     * Marks a pending purchase failed and refunds it.
     *
     * @return true if a refund was issued
     */
    public boolean failAndRefund(String purchaseId) throws SQLException {
        return Database.inTransaction(c -> {
            long user;
            int cost;
            try (PreparedStatement ps = c.prepareStatement("SELECT user, cost FROM shop_purchases "
                    + "WHERE purchase_id = ? AND status = ? FOR UPDATE")) {
                ps.setString(1, purchaseId);
                ps.setString(2, PENDING);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    user = rs.getLong(1);
                    cost = rs.getInt(2);
                }
            }
            EconomyService.creditIn(c, user, cost, "shop:refund", purchaseId, "refund:" + purchaseId);
            setStatus(c, purchaseId, FAILED);
            return true;
        });
    }

    /**
     * @return purchases still pending after {@code olderThanMs}, e.g. because
     *         the bot restarted before Discord answered
     */
    public List<Pending> findStalePending(long olderThanMs) throws SQLException {
        return Database.inTransaction(c -> {
            List<Pending> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT purchase_id, user, role_id FROM shop_purchases "
                    + "WHERE status = ? AND created_at < ?")) {
                ps.setString(1, PENDING);
                ps.setLong(2, System.currentTimeMillis() - olderThanMs);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(new Pending(rs.getString(1), rs.getLong(2), rs.getLong(3)));
                }
            }
            return list;
        });
    }

    private static void setStatus(java.sql.Connection c, String purchaseId, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE shop_purchases SET status = ?, updated_at = ? WHERE purchase_id = ?")) {
            ps.setString(1, status);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, purchaseId);
            ps.executeUpdate();
        }
    }
}

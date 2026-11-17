package economy;

import database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Pay for a permanent colour in installments. Each installment is final and
 * the plan cannot be transferred. When the total is reached the plan moves to
 * GRANTING until Discord confirms the role.
 */
public final class InstallmentService {

    public static final int PRICE = 2500;
    private static final long PERMANENT_MS = 99999L * 86400000L;
    private static final InstallmentService INSTANCE = new InstallmentService();

    private InstallmentService() { }

    public static InstallmentService getInstance() {
        return INSTANCE;
    }

    /** A plan snapshot. */
    public static final class Plan {
        public final long id;
        public final long user;
        public final long roleId;
        public final String colour;
        public final int target;
        public final int paid;
        public final String status;

        Plan(long id, long user, long roleId, String colour, int target, int paid, String status) {
            this.id = id;
            this.user = user;
            this.roleId = roleId;
            this.colour = colour;
            this.target = target;
            this.paid = paid;
            this.status = status;
        }
    }

    /** @return the user's unfinished plan (ACTIVE or GRANTING), or null */
    public Plan current(long user) throws SQLException {
        return Database.inTransaction(c -> load(c, user, false));
    }

    /** @return the new plan, or null if the user already has an unfinished one */
    public Plan start(long user, long roleId, String colour) throws SQLException {
        return Database.inTransaction(c -> {
            EconomyService.lockWallet(c, user);
            if (load(c, user, true) != null) return null;
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO colour_plans (user, role_id, colour, target, "
                    + "paid, status, created_at) VALUES (?, ?, ?, ?, 0, 'ACTIVE', ?)")) {
                ps.setLong(1, user);
                ps.setLong(2, roleId);
                ps.setString(3, colour);
                ps.setInt(4, PRICE);
                ps.setLong(5, System.currentTimeMillis());
                ps.executeUpdate();
            }
            return load(c, user, false);
        });
    }

    /**
     * Pays up to {@code amount}, never more than the plan still needs.
     *
     * @return the plan after payment, or null if there is no active plan
     */
    public Plan pay(long user, int amount, String opId) throws SQLException, InsufficientFundsException {
        try {
            return Database.inTransaction(c -> {
                EconomyService.lockWallet(c, user);
                Plan p = load(c, user, true);
                if (p == null || !p.status.equals("ACTIVE")) return null;
                int charge = Math.min(amount, p.target - p.paid);
                if (charge <= 0) return p;
                EconomyService.debitIn(c, user, charge, "installment", Long.toString(p.id), opId);
                boolean full = p.paid + charge >= p.target;
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE colour_plans SET paid = paid + ?, status = ? WHERE id = ?")) {
                    ps.setInt(1, charge);
                    ps.setString(2, full ? "GRANTING" : "ACTIVE");
                    ps.setLong(3, p.id);
                    ps.executeUpdate();
                }
                return load(c, user, false);
            });
        } catch (EconomyService.InsufficientFundsRuntime e) {
            throw new InsufficientFundsException(0, amount);
        }
    }

    /** Discord confirmed the role: the colour becomes permanent. */
    public boolean markGranted(long planId) throws SQLException {
        return Database.inTransaction(c -> {
            long user;
            long roleId;
            String colour;
            try (PreparedStatement ps = c.prepareStatement("SELECT user, role_id, colour FROM colour_plans "
                    + "WHERE id = ? AND status = 'GRANTING' FOR UPDATE")) {
                ps.setLong(1, planId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    user = rs.getLong(1);
                    roleId = rs.getLong(2);
                    colour = rs.getString(3);
                }
            }
            EconomyService.lockWallet(c, user);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE economy SET role_expiry = ?, role_colour = ?, role_id = ? WHERE user = ?")) {
                ps.setLong(1, System.currentTimeMillis() + PERMANENT_MS);
                ps.setString(2, colour);
                ps.setLong(3, roleId);
                ps.setLong(4, user);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE colour_plans SET status = 'COMPLETE' WHERE id = ?")) {
                ps.setLong(1, planId);
                ps.executeUpdate();
            }
            return true;
        });
    }

    private static Plan load(Connection c, long user, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id, user, role_id, colour, target, paid, status "
                + "FROM colour_plans WHERE user = ? AND status IN ('ACTIVE', 'GRANTING') ORDER BY id DESC LIMIT 1"
                + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Plan(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getString(4),
                        rs.getInt(5), rs.getInt(6), rs.getString(7)) : null;
            }
        }
    }

}

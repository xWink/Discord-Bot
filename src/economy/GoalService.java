package economy;

import database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Community goals. A contribution debits the wallet and advances the goal in
 * one transaction. The goal can only be reached once, and cancelling before
 * delivery reverses every contribution through the ledger.
 */
public final class GoalService {

    public static final String ACTIVE = "ACTIVE";
    public static final String REACHED = "REACHED";
    public static final String CANCELLED = "CANCELLED";

    private static final GoalService INSTANCE = new GoalService();

    private GoalService() { }

    public static GoalService getInstance() {
        return INSTANCE;
    }

    /** Snapshot of a goal for display. */
    public static final class Goal {
        public final long id;
        public final String title;
        public final int target;
        public final int raised;
        public final String status;
        public final int contributors;

        Goal(long id, String title, int target, int raised, String status, int contributors) {
            this.id = id;
            this.title = title;
            this.target = target;
            this.raised = raised;
            this.status = status;
            this.contributors = contributors;
        }
    }

    /** What a contribution did. */
    public static final class Outcome {
        public final int charged;
        public final boolean reachedNow;
        public final Goal goal;

        Outcome(int charged, boolean reachedNow, Goal goal) {
            this.charged = charged;
            this.reachedNow = reachedNow;
            this.goal = goal;
        }
    }

    /**
     * @return the active goal, or the most recent one, or null if none exist
     */
    public Goal current() throws SQLException {
        return Database.inTransaction(c -> load(c, "SELECT id FROM community_goals ORDER BY (status = 'ACTIVE') DESC, "
                + "id DESC LIMIT 1", false));
    }

    public Goal create(String title, int target) throws SQLException {
        if (target <= 0) throw new IllegalArgumentException("Target must be positive");
        return Database.inTransaction(c -> {
            if (load(c, "SELECT id FROM community_goals WHERE status = 'ACTIVE' LIMIT 1", false) != null) {
                return null; // one active goal at a time
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO community_goals (title, target, raised, "
                    + "status, created_at) VALUES (?, ?, 0, ?, ?)")) {
                ps.setString(1, title);
                ps.setInt(2, target);
                ps.setString(3, ACTIVE);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            }
            return load(c, "SELECT id FROM community_goals WHERE status = 'ACTIVE' LIMIT 1", false);
        });
    }

    /**
     * Contributes up to {@code amount}, charging only what the goal still
     * needs if it is nearly full.
     *
     * @return the outcome, or null if there is no active goal
     */
    public Outcome contribute(long user, int amount, String opId) throws SQLException, InsufficientFundsException {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        try {
            return Database.inTransaction(c -> {
                Goal goal = load(c, "SELECT id FROM community_goals WHERE status = 'ACTIVE' LIMIT 1", true);
                if (goal == null) return null;
                int charge = Math.min(amount, goal.target - goal.raised);
                if (charge <= 0) return new Outcome(0, false, goal);

                EconomyService.debitIn(c, user, charge, "goal", Long.toString(goal.id), opId);
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO goal_contributions (goal_id, user, "
                        + "amount, op_id, created_at) VALUES (?, ?, ?, ?, ?)")) {
                    ps.setLong(1, goal.id);
                    ps.setLong(2, user);
                    ps.setInt(3, charge);
                    ps.setString(4, opId);
                    ps.setLong(5, System.currentTimeMillis());
                    ps.executeUpdate();
                }
                Achievements.awardIn(c, user, "goal-helper");
                boolean reached = goal.raised + charge >= goal.target;
                try (PreparedStatement ps = c.prepareStatement("UPDATE community_goals SET raised = raised + ?, "
                        + "status = ?, reached_at = ? WHERE id = ?")) {
                    ps.setInt(1, charge);
                    ps.setString(2, reached ? REACHED : ACTIVE);
                    if (reached) ps.setLong(3, System.currentTimeMillis());
                    else ps.setNull(3, java.sql.Types.BIGINT);
                    ps.setLong(4, goal.id);
                    ps.executeUpdate();
                }
                return new Outcome(charge, reached, load(c, "SELECT id FROM community_goals WHERE id = " + goal.id,
                        false));
            });
        } catch (EconomyService.InsufficientFundsRuntime e) {
            throw new InsufficientFundsException(0, amount);
        }
    }

    /**
     * Cancels a goal that has not been delivered and gives every contribution
     * back through the ledger.
     *
     * @return the number of contributions reversed, or -1 if no goal was cancellable
     */
    public int cancelCurrent() throws SQLException {
        return Database.inTransaction(c -> {
            Goal goal = load(c, "SELECT id FROM community_goals WHERE status IN ('ACTIVE', 'REACHED') "
                    + "ORDER BY id DESC LIMIT 1", true);
            if (goal == null) return -1;
            int reversed = 0;
            try (PreparedStatement ps = c.prepareStatement("SELECT id, user, amount FROM goal_contributions "
                    + "WHERE goal_id = ? AND reversed = 0")) {
                ps.setLong(1, goal.id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        EconomyService.creditIn(c, rs.getLong(2), rs.getInt(3), "goal:refund",
                                Long.toString(goal.id), "goal-refund:" + rs.getLong(1));
                        reversed++;
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE goal_contributions SET reversed = 1 WHERE goal_id = ?")) {
                ps.setLong(1, goal.id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE community_goals SET status = ? WHERE id = ?")) {
                ps.setString(1, CANCELLED);
                ps.setLong(2, goal.id);
                ps.executeUpdate();
            }
            return reversed;
        });
    }

    private static Goal load(Connection c, String idQuery, boolean lock) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement(idQuery); ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;
            id = rs.getLong(1);
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT title, target, raised, status FROM community_goals "
                + "WHERE id = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int contributors;
                try (PreparedStatement cp = c.prepareStatement("SELECT COUNT(DISTINCT user) FROM goal_contributions "
                        + "WHERE goal_id = ? AND reversed = 0")) {
                    cp.setLong(1, id);
                    try (ResultSet crs = cp.executeQuery()) {
                        crs.next();
                        contributors = crs.getInt(1);
                    }
                }
                return new Goal(id, rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getString(4), contributors);
            }
        }
    }
}

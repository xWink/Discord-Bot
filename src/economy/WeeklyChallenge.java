package economy;

import arcade.PunchCard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Weekly challenge rewards. It pays once per player per week no matter how it
 * is completed: a quiz run (10 GC, 15 GC for a perfect run) or attending a
 * hosted event (10 GC).
 */
public final class WeeklyChallenge {

    public static final int BASE_REWARD = 10;
    public static final int PERFECT_REWARD = 15;
    public static final int MAX_ATTEMPTS = 2;

    private WeeklyChallenge() { }

    public static String opId(long user, int week) {
        return "weekly:" + user + ":" + week;
    }

    public static boolean isDoneIn(Connection c, long user, int week) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM economy_ledger WHERE op_id = ? AND user = ?")) {
            ps.setString(1, opId(user, week));
            ps.setLong(2, user);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Pays the weekly reward if it has not been paid this week.
     *
     * @return GC paid, 0 if already completed
     */
    public static int payIn(Connection c, long user, int reward, String how) throws SQLException {
        int week = PunchCard.currentWeek();
        EconomyService.lockWallet(c, user);
        if (isDoneIn(c, user, week)) return 0;
        EconomyService.creditIn(c, user, reward, "weekly", how, opId(user, week));
        return reward;
    }

    public static int completeViaEventIn(Connection c, long user) throws SQLException {
        return payIn(c, user, BASE_REWARD, "event");
    }
}

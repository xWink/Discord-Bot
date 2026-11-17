package arcade;

import database.Database;
import economy.EconomyService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Weekly punch card: finish a daily activity (arcade or bang) on three
 * different days in a week for 10 GC. Missed days are allowed.
 */
public final class PunchCard {

    public static final int DAYS_NEEDED = 3;
    public static final int REWARD = 10;
    private static final ZoneId ZONE = ZoneId.of("America/New_York");

    private PunchCard() { }

    /** Monday-based week number for a local epoch day. */
    static int weekOf(long epochDay) {
        return (int) Math.floorDiv(epochDay + 3, 7L);
    }

    public static int currentWeek() {
        return weekOf(LocalDate.now(ZONE).toEpochDay());
    }

    /**
     * Punches today. Pays the reward the moment the third distinct day of the
     * week is reached, once per week.
     *
     * @return true if this punch paid the weekly reward
     */
    public static boolean record(long user) throws SQLException {
        long today = LocalDate.now(ZONE).toEpochDay();
        int week = weekOf(today);
        return Database.inTransaction(c -> {
            EconomyService.lockWallet(c, user); // serialize one player's punches
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT IGNORE INTO arcade_days (user, day, week) VALUES (?, ?, ?)")) {
                ps.setLong(1, user);
                ps.setLong(2, today);
                ps.setInt(3, week);
                ps.executeUpdate();
            }
            if (days(c, user, week) < DAYS_NEEDED) return false;
            String opId = "punch:" + user + ":" + week;
            boolean already;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM economy_ledger WHERE op_id = ? AND user = ?")) {
                ps.setString(1, opId);
                ps.setLong(2, user);
                try (ResultSet rs = ps.executeQuery()) {
                    already = rs.next();
                }
            }
            if (already) return false;
            EconomyService.creditIn(c, user, REWARD, "punchcard", Integer.toString(week), opId);
            return true;
        });
    }

    /**
     * @return distinct days punched this week
     */
    public static int daysThisWeek(long user) throws SQLException {
        int week = currentWeek();
        return Database.inTransaction(c -> days(c, user, week));
    }

    private static int days(java.sql.Connection c, long user, int week) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM arcade_days WHERE user = ? AND week = ?")) {
            ps.setLong(1, user);
            ps.setInt(2, week);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}

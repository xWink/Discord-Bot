package courses;

import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Who wants a morning summary by direct message, and when. */
public final class DigestStore {

    public static final int DEFAULT_MINUTE = 8 * 60;
    /** A digest more than this many minutes after its time (the bot was off) is skipped for the day. */
    public static final int STALE_MINUTES = 180;

    private DigestStore() { }

    /** @return the minute of the day (0 to 1439) the person's digest is sent, or -1 if it is off */
    public static int minute(long user) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT enabled, minute_of_day FROM digest_prefs WHERE user = ?")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) != 0 ? rs.getInt(2) : -1;
                }
            }
        });
    }

    public static void set(long user, boolean enabled, int minuteOfDay) throws SQLException {
        Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO digest_prefs (user, enabled, minute_of_day) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE enabled = VALUES(enabled), minute_of_day = VALUES(minute_of_day)")) {
                ps.setLong(1, user);
                ps.setInt(2, enabled ? 1 : 0);
                ps.setInt(3, minuteOfDay);
                ps.executeUpdate();
            }
            return null;
        });
    }

    /**
     * @param today days since 1970 on the campus clock
     * @param nowMinute the minute of the day now
     * @return people whose time has come and who have not had today's digest; each is marked as sent
     */
    public static List<Long> claimDue(long today, int nowMinute) throws SQLException {
        return Database.inTransaction(c -> {
            List<Long> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT user, minute_of_day FROM digest_prefs WHERE enabled = 1 "
                    + "AND last_sent_day < ? AND minute_of_day <= ? FOR UPDATE")) {
                ps.setLong(1, today);
                ps.setInt(2, nowMinute);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(rs.getLong(1));
                }
            }
            for (long user : out) {
                try (PreparedStatement ps = c.prepareStatement("UPDATE digest_prefs SET last_sent_day = ? WHERE user = ?")) {
                    ps.setLong(1, today);
                    ps.setLong(2, user);
                    ps.executeUpdate();
                }
            }
            return out;
        });
    }
}

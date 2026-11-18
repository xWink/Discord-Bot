package courses;

import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** People's reminders. Each is sent once: claiming a due reminder marks it done in the same transaction. */
public final class Reminders {

    /** Most reminders one person can have waiting. */
    public static final int MAX_PENDING = 25;
    public static final int MAX_TEXT = 300;

    private Reminders() { }

    /** A reminder. */
    public static final class Reminder {
        public final long id;
        public final long user;
        public final long channel;
        public final String text;
        public final long due;
        /** True to post it in the channel it was made in instead of a direct message. */
        public final boolean here;

        Reminder(long id, long user, long channel, String text, long due, boolean here) {
            this.id = id;
            this.user = user;
            this.channel = channel;
            this.text = text;
            this.due = due;
            this.here = here;
        }
    }

    private static Reminder read(ResultSet rs) throws SQLException {
        return new Reminder(rs.getLong("id"), rs.getLong("user"), rs.getLong("channel"), rs.getString("text"),
                rs.getLong("due"), rs.getInt("here") != 0);
    }

    /** @return the new reminder's number, or -1 if the person already has {@link #MAX_PENDING} waiting */
    public static long create(long user, long channel, String text, long due, boolean here) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM reminders WHERE user = ? AND done = 0")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) >= MAX_PENDING) return -1L;
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO reminders (user, channel, text, due, here, done, created) VALUES (?, ?, ?, ?, ?, 0, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, user);
                ps.setLong(2, channel);
                ps.setString(3, text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text);
                ps.setLong(4, due);
                ps.setInt(5, here ? 1 : 0);
                ps.setLong(6, System.currentTimeMillis());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            }
        });
    }

    /** @return the person's waiting reminders, soonest first */
    public static List<Reminder> pending(long user) throws SQLException {
        return Database.inTransaction(c -> {
            List<Reminder> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM reminders WHERE user = ? AND done = 0 ORDER BY due")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(read(rs));
                }
            }
            return out;
        });
    }

    /** @return true if it was theirs and still waiting */
    public static boolean cancel(long user, long id) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE reminders SET done = 1 WHERE id = ? AND user = ? AND done = 0")) {
                ps.setLong(1, id);
                ps.setLong(2, user);
                return ps.executeUpdate() > 0;
            }
        });
    }

    /** @return reminders that have come due, now marked done so no other pass sends them again */
    public static List<Reminder> claimDue(long now, int limit) throws SQLException {
        return Database.inTransaction(c -> {
            List<Reminder> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM reminders WHERE done = 0 AND due <= ? ORDER BY due LIMIT ? FOR UPDATE")) {
                ps.setLong(1, now);
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(read(rs));
                }
            }
            for (Reminder r : out) {
                try (PreparedStatement ps = c.prepareStatement("UPDATE reminders SET done = 1 WHERE id = ?")) {
                    ps.setLong(1, r.id);
                    ps.executeUpdate();
                }
            }
            return out;
        });
    }
}

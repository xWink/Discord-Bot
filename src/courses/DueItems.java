package courses;

import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Assignments and tests with a due time, per person and course. Each can carry a reminder, which goes away with it. */
public final class DueItems {

    /** Most open items one person can have. */
    public static final int MAX_OPEN = 60;

    private DueItems() { }

    /** One thing that is due. */
    public static final class Item {
        public final long id;
        public final long user;
        public final String code;
        public final String title;
        public final long due;
        public final long reminderId;

        Item(long id, long user, String code, String title, long due, long reminderId) {
            this.id = id;
            this.user = user;
            this.code = code;
            this.title = title;
            this.due = due;
            this.reminderId = reminderId;
        }
    }

    private static Item read(ResultSet rs) throws SQLException {
        long reminder = rs.getLong("reminder_id");
        return new Item(rs.getLong("id"), rs.getLong("user"), rs.getString("code"), rs.getString("title"), rs.getLong("due"),
                rs.wasNull() ? 0 : reminder);
    }

    /** @return the new item's number, or -1 if the person already has {@link #MAX_OPEN} open */
    public static long add(long user, String code, String title, long due, long reminderId) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM due_items WHERE user = ? AND done = 0")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) >= MAX_OPEN) return -1L;
                }
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO due_items (user, code, title, due, reminder_id, created) "
                    + "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, user);
                ps.setString(2, code);
                ps.setString(3, title.length() > 80 ? title.substring(0, 80) : title);
                ps.setLong(4, due);
                if (reminderId > 0) ps.setLong(5, reminderId);
                else ps.setNull(5, java.sql.Types.BIGINT);
                ps.setLong(6, System.currentTimeMillis());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            }
        });
    }

    /** @param code only this course, or null for all */
    public static List<Item> open(long user, String code) throws SQLException {
        return Database.inTransaction(c -> {
            List<Item> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM due_items WHERE user = ? AND done = 0"
                    + (code == null ? "" : " AND code = ?") + " ORDER BY due")) {
                ps.setLong(1, user);
                if (code != null) ps.setString(2, code);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(read(rs));
                }
            }
            return out;
        });
    }

    /** Finishes or deletes an item. @return the item, or null if it is not theirs or already done; its reminder is cancelled */
    public static Item close(long user, long id, boolean delete) throws SQLException {
        Item item = Database.inTransaction(c -> {
            Item found = null;
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM due_items WHERE id = ? AND user = ? AND done = 0 FOR UPDATE")) {
                ps.setLong(1, id);
                ps.setLong(2, user);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) found = read(rs);
                }
            }
            if (found == null) return null;
            try (PreparedStatement ps = c.prepareStatement(delete ? "DELETE FROM due_items WHERE id = ?" : "UPDATE due_items SET done = 1 WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            return found;
        });
        if (item != null && item.reminderId > 0) Reminders.cancel(user, item.reminderId);
        return item;
    }
}

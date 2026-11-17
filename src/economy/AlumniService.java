package economy;

import database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Alumni is an honour moderators bestow. It cannot be bought, never expires, and is shown automatically on the
 * profile card and next to the member's name, with no need to equip anything.
 */
public final class AlumniService {

    private static final AlumniService INSTANCE = new AlumniService();

    private AlumniService() { }

    public static AlumniService getInstance() {
        return INSTANCE;
    }

    /** A member's alumni record. */
    public static final class Record {
        public final long user;
        public final long grantedBy;
        public final String note;
        public final long grantedAt;

        Record(long user, long grantedBy, String note, long grantedAt) {
            this.user = user;
            this.grantedBy = grantedBy;
            this.note = note;
            this.grantedAt = grantedAt;
        }

        /** The "Class of" year: the year the honour was granted. */
        public int classYear() {
            return Instant.ofEpochMilli(grantedAt).atZone(ZoneId.of("America/New_York")).getYear();
        }
    }

    /**
     * @return true if newly granted, false if they were already alumni (the note is updated)
     */
    public boolean grant(long user, long by, String note) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO alumni (user, granted_by, note, granted_at) "
                    + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE note = VALUES(note)")) {
                ps.setLong(1, user);
                ps.setLong(2, by);
                ps.setString(3, note == null || note.isEmpty() ? null : note);
                ps.setLong(4, System.currentTimeMillis());
                return ps.executeUpdate() == 1; // 1 = inserted, 2 = updated an existing row
            }
        });
    }

    /** @return true if they were alumni and are not any more */
    public boolean revoke(long user) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM alumni WHERE user = ?")) {
                ps.setLong(1, user);
                return ps.executeUpdate() > 0;
            }
        });
    }

    public Record get(long user) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT user, granted_by, note, granted_at FROM alumni WHERE user = ?")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? new Record(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4)) : null;
                }
            }
        });
    }

    /** @return the most recent alumni, newest first */
    public List<Record> list(int limit) throws SQLException {
        return Database.inTransaction(c -> {
            List<Record> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT user, granted_by, note, granted_at FROM alumni "
                    + "ORDER BY granted_at DESC LIMIT ?")) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(new Record(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4)));
                }
            }
            return list;
        });
    }

    /** @return the class year, or 0 if not alumni; for use inside another transaction */
    public static int yearIn(Connection c, long user) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT granted_at FROM alumni WHERE user = ?")) {
            ps.setLong(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Instant.ofEpochMilli(rs.getLong(1)).atZone(ZoneId.of("America/New_York")).getYear() : 0;
            }
        }
    }
}

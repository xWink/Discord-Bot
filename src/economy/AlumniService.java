package economy;

import database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        private final int classYear;

        Record(long user, long grantedBy, String note, long grantedAt, int classYear) {
            this.user = user;
            this.grantedBy = grantedBy;
            this.note = note;
            this.grantedAt = grantedAt;
            this.classYear = classYear;
        }

        /** The "Class of" year a mod chose, or 0 if none was given. */
        public int classYear() {
            return classYear;
        }

        /** "Class of 2019", or an empty string when no year was given. */
        public String classLabel() {
            return classYear > 0 ? "Class of " + classYear : "";
        }
    }

    /**
     * Grants alumni. The year is optional: pass 0 to show no "Class of" line. Granting again updates the note,
     * and updates the year only if a new one is given.
     *
     * @return true if newly granted, false if they were already alumni
     */
    public boolean grant(long user, long by, String note, int classYear) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO alumni (user, granted_by, note, granted_at, class_year) "
                    + "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE note = VALUES(note), "
                    + "class_year = COALESCE(VALUES(class_year), class_year)")) {
                ps.setLong(1, user);
                ps.setLong(2, by);
                ps.setString(3, note == null || note.isEmpty() ? null : note);
                ps.setLong(4, System.currentTimeMillis());
                if (classYear > 0) ps.setInt(5, classYear);
                else ps.setNull(5, java.sql.Types.INTEGER);
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
                    "SELECT user, granted_by, note, granted_at, COALESCE(class_year, 0) FROM alumni WHERE user = ?")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? new Record(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4), rs.getInt(5)) : null;
                }
            }
        });
    }

    /** @return the most recent alumni, newest first */
    public List<Record> list(int limit) throws SQLException {
        return Database.inTransaction(c -> {
            List<Record> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT user, granted_by, note, granted_at, COALESCE(class_year, 0) FROM alumni "
                    + "ORDER BY granted_at DESC LIMIT ?")) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(new Record(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4), rs.getInt(5)));
                }
            }
            return list;
        });
    }

    /** @return true if the member is alumni; for use inside another transaction */
    public static boolean isAlumniIn(Connection c, long user) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM alumni WHERE user = ?")) {
            ps.setLong(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** @return the class year a mod chose, or 0 if none; for use inside another transaction */
    public static int classYearIn(Connection c, long user) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(class_year, 0) FROM alumni WHERE user = ?")) {
            ps.setLong(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}

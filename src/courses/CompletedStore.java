package courses;

import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/** The courses each person says they have finished. Used to work out what they can take next. */
public final class CompletedStore {

    /** Most courses one person can list. */
    public static final int MAX = 300;

    private CompletedStore() { }

    public static Set<String> list(long user) throws SQLException {
        return Database.inTransaction(c -> {
            Set<String> out = new TreeSet<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT code FROM completed_courses WHERE user = ?")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(rs.getString(1));
                }
            }
            return out;
        });
    }

    /** @return how many were new (ones already listed are skipped); stops at {@link #MAX} */
    public static int add(long user, Collection<String> codes) throws SQLException {
        return Database.inTransaction(c -> {
            int have = list(c, user).size();
            int added = 0;
            for (String code : codes) {
                if (have + added >= MAX) break;
                try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO completed_courses (user, code) VALUES (?, ?)")) {
                    ps.setLong(1, user);
                    ps.setString(2, code);
                    added += ps.executeUpdate();
                }
            }
            return added;
        });
    }

    private static Set<String> list(java.sql.Connection c, long user) throws SQLException {
        Set<String> out = new TreeSet<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT code FROM completed_courses WHERE user = ?")) {
            ps.setLong(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString(1));
            }
        }
        return out;
    }

    /** @return how many were removed */
    public static int remove(long user, Collection<String> codes) throws SQLException {
        return Database.inTransaction(c -> {
            int removed = 0;
            for (String code : codes) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM completed_courses WHERE user = ? AND code = ?")) {
                    ps.setLong(1, user);
                    ps.setString(2, code);
                    removed += ps.executeUpdate();
                }
            }
            return removed;
        });
    }

    public static int clear(long user) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM completed_courses WHERE user = ?")) {
                ps.setLong(1, user);
                return ps.executeUpdate();
            }
        });
    }
}

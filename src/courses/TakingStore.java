package courses;

import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** The courses each person is taking right now, and who else is taking a course (only people who chose to be seen). */
public final class TakingStore {

    /** Most courses one person can list as current. */
    public static final int MAX = 12;

    private TakingStore() { }

    public static Set<String> list(long user) throws SQLException {
        return Database.inTransaction(c -> list(c, user));
    }

    private static Set<String> list(java.sql.Connection c, long user) throws SQLException {
        Set<String> out = new TreeSet<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT code FROM taking_courses WHERE user = ?")) {
            ps.setLong(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString(1));
            }
        }
        return out;
    }

    /** @return how many were new; stops at {@link #MAX} */
    public static int add(long user, Collection<String> codes) throws SQLException {
        return Database.inTransaction(c -> {
            int have = list(c, user).size();
            int added = 0;
            for (String code : codes) {
                if (have + added >= MAX) break;
                try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO taking_courses (user, code, since) VALUES (?, ?, ?)")) {
                    ps.setLong(1, user);
                    ps.setString(2, code);
                    ps.setLong(3, System.currentTimeMillis());
                    added += ps.executeUpdate();
                }
            }
            return added;
        });
    }

    /** @return how many were removed */
    public static int remove(long user, Collection<String> codes) throws SQLException {
        return Database.inTransaction(c -> removeIn(c, user, codes));
    }

    private static int removeIn(java.sql.Connection c, long user, Collection<String> codes) throws SQLException {
        int removed = 0;
        for (String code : codes) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM taking_courses WHERE user = ? AND code = ?")) {
                ps.setLong(1, user);
                ps.setString(2, code);
                removed += ps.executeUpdate();
            }
        }
        return removed;
    }

    /** Moves courses from current to finished, in one step. @return how many were taking */
    public static int finish(long user, Collection<String> codes) throws SQLException {
        return Database.inTransaction(c -> {
            Set<String> taking = list(c, user);
            List<String> moving = new ArrayList<>();
            for (String code : codes) if (taking.contains(code)) moving.add(code);
            for (String code : moving) {
                try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO completed_courses (user, code) VALUES (?, ?)")) {
                    ps.setLong(1, user);
                    ps.setString(2, code);
                    ps.executeUpdate();
                }
            }
            removeIn(c, user, moving);
            return moving.size();
        });
    }

    /** Who is taking a course. */
    public static final class Classmates {
        /** People who chose to be seen. */
        public final List<Long> visible = new ArrayList<>();
        /** Everyone taking it, seen or not. */
        public int total;
    }

    public static Classmates classmates(String code, int limit) throws SQLException {
        return Database.inTransaction(c -> {
            Classmates out = new Classmates();
            try (PreparedStatement ps = c.prepareStatement("SELECT t.user, COALESCE(p.share_classes, 0) FROM taking_courses t "
                    + "LEFT JOIN profile_prefs p ON p.user = t.user WHERE t.code = ? ORDER BY t.since")) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.total++;
                        if (rs.getInt(2) != 0 && out.visible.size() < limit) out.visible.add(rs.getLong(1));
                    }
                }
            }
            return out;
        });
    }
}

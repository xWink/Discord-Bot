package economy;

import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Per-person profile card settings. Animation is off until someone turns it on. */
public final class ProfilePrefs {

    private ProfilePrefs() { }

    public static boolean isAnimated(long user) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT animated FROM profile_prefs WHERE user = ?")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) != 0;
                }
            }
        });
    }

    public static void setAnimated(long user, boolean animated) throws SQLException {
        Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO profile_prefs (user, animated) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE animated = VALUES(animated)")) {
                ps.setLong(1, user);
                ps.setInt(2, animated ? 1 : 0);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private static boolean flag(long user, String column) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + column + " FROM profile_prefs WHERE user = ?")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) != 0;
                }
            }
        });
    }

    private static void setFlag(long user, String column, boolean on) throws SQLException {
        Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO profile_prefs (user, " + column + ") VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE " + column + " = VALUES(" + column + ")")) {
                ps.setLong(1, user);
                ps.setInt(2, on ? 1 : 0);
                ps.executeUpdate();
            }
            return null;
        });
    }

    /** True if the person has asked for no animations anywhere: every result comes as plain text. */
    public static boolean isReducedMotion(long user) throws SQLException {
        return flag(user, "reduce_motion");
    }

    public static void setReducedMotion(long user, boolean on) throws SQLException {
        setFlag(user, "reduce_motion", on);
    }

    /** True if the person lets classmates see which courses they are taking. Off until they turn it on. */
    public static boolean isSharingClasses(long user) throws SQLException {
        return flag(user, "share_classes");
    }

    public static void setSharingClasses(long user, boolean on) throws SQLException {
        setFlag(user, "share_classes", on);
    }
}

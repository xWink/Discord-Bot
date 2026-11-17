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
}

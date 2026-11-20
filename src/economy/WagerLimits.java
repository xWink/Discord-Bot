package economy;

import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Responsible-play guards shared by every paid game: a per-account opt-out.
 * There is no cap on losses; the games warn about large stakes instead.
 */
public final class WagerLimits {

    private WagerLimits() { }

    public static boolean isPaidPlayEnabled(long user) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT paid_play FROM wager_settings WHERE user = ?")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    return !rs.next() || rs.getInt(1) != 0;
                }
            }
        });
    }

    public static void setPaidPlay(long user, boolean enabled) throws SQLException {
        Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO wager_settings (user, paid_play) "
                    + "VALUES (?, ?) ON DUPLICATE KEY UPDATE paid_play = VALUES(paid_play)")) {
                ps.setLong(1, user);
                ps.setInt(2, enabled ? 1 : 0);
                ps.executeUpdate();
            }
            return null;
        });
    }

    /**
     * @return null if the player may stake this amount, otherwise a message
     *         explaining why not
     */
    public static String check(long user, int stake) throws SQLException {
        if (!isPaidPlayEnabled(user)) {
            return "Paid play is turned off for your account. Say `!paidplay on` to turn it back on.";
        }
        return null;
    }
}

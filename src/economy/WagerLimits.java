package economy;

import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Responsible-play guards shared by every paid game: a per-account opt-out and
 * a cap on how much a player can lose in a rolling 24 hours.
 */
public final class WagerLimits {

    /** Most a player can lose to paid games in 24 hours. */
    public static final int DAILY_LOSS_CAP = 50;

    private static final long DAY_MS = 24L * 60 * 60 * 1000;

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
     * @return GC lost to paid games in the last 24 hours
     */
    public static int lossesToday(long user) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(-SUM(delta), 0) FROM economy_ledger "
                    + "WHERE user = ? AND reason IN ('blackjack', 'duel', 'dice', 'hilo') AND delta < 0 AND created_at >= ?")) {
                ps.setLong(1, user);
                ps.setLong(2, System.currentTimeMillis() - DAY_MS);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
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
        int lost = lossesToday(user);
        if (lost + stake > DAILY_LOSS_CAP) {
            return "That would take you past the " + DAILY_LOSS_CAP + " GC daily loss limit "
                    + "(you have lost " + lost + " GC in the last 24 hours).";
        }
        return null;
    }
}

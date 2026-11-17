package economy;

import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The single clock and eligibility rule for daily rewards, shared by
 * {@code !bang}, {@code !daily}, and the arcade. The claim timestamp and the
 * payout are written in the same transaction, so a queued gameplay-stat
 * batch can never leave a player paid-but-still-eligible.
 */
public final class DailyService {

    /** Time that must pass between two daily claims. */
    public static final long COOLDOWN_MS = 24L * 60 * 60 * 1000;

    /** GC paid for a daily claim. */
    public static final int DAILY_REWARD = 5;

    private static final DailyService INSTANCE = new DailyService();

    private DailyService() { }

    public static DailyService getInstance() {
        return INSTANCE;
    }

    /**
     * Claims the daily reward if the player is eligible.
     *
     * @param user   the claiming player
     * @param source which activity triggered the claim (bang, arcade, ...)
     * @param now    the current time in ms since epoch
     * @return true if this call paid the reward, false if already claimed
     */
    public boolean claim(long user, String source, long now) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT IGNORE INTO bang (user, tries, deaths, jams, last_played, streak) "
                            + "VALUES (?, 0, 0, 0, 0, 0)")) {
                ps.setLong(1, user);
                ps.executeUpdate();
            }
            long lastDaily;
            try (PreparedStatement ps = c.prepareStatement("SELECT last_daily FROM bang WHERE user = ? FOR UPDATE")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    lastDaily = rs.getLong(1);
                }
            }
            if (now - lastDaily < COOLDOWN_MS) return false;
            try (PreparedStatement ps = c.prepareStatement("UPDATE bang SET last_daily = ? WHERE user = ?")) {
                ps.setLong(1, now);
                ps.setLong(2, user);
                ps.executeUpdate();
            }
            EconomyService.creditIn(c, user, DAILY_REWARD, "daily", source, "daily:" + user + ":" + now);
            return true;
        });
    }

    /**
     * @return the time in ms since epoch at which the player may claim again,
     *         or 0 if they have never claimed
     */
    public long nextEligibleAt(long user) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT last_daily FROM bang WHERE user = ?")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) + COOLDOWN_MS : 0L;
                }
            }
        });
    }
}

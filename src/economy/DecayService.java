package economy;

import database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies weekly decay. Overdue weeks are settled inside every wallet lock, so
 * no balance changes (including both sides of a gift) before it is current.
 * Decay stays off until an admin publishes a first cutoff week.
 */
public final class DecayService {

    private static final long WEEK_MS = 7L * 24 * 60 * 60 * 1000;
    /** A Monday 00:00 UTC; weeks are counted from here. */
    private static final long EPOCH_MS = 1578268800000L;
    private static final String FIRST_WEEK = "decay_first_week";

    private static Long cachedFirstWeek;
    private static boolean loaded;

    private DecayService() { }

    public static int weekIndex(long now) {
        return (int) ((now - EPOCH_MS) / WEEK_MS);
    }

    public static long weekStart(int week) {
        return EPOCH_MS + week * WEEK_MS;
    }

    /**
     * @return the first week that charges decay, or null while it is off
     */
    public static synchronized Long getFirstWeek(Connection c) throws SQLException {
        if (!loaded) {
            try (PreparedStatement ps = c.prepareStatement("SELECT value FROM economy_settings WHERE name = ?")) {
                ps.setString(1, FIRST_WEEK);
                try (ResultSet rs = ps.executeQuery()) {
                    cachedFirstWeek = rs.next() ? rs.getLong(1) : null;
                }
            }
            loaded = true;
        }
        return cachedFirstWeek;
    }

    public static Long getFirstWeek() throws SQLException {
        return Database.inTransaction(DecayService::getFirstWeek);
    }

    /**
     * Publishes the first cutoff. Callers should leave a preview week before it.
     */
    public static void setFirstWeek(int week) throws SQLException {
        Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO economy_settings (name, value) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE value = VALUES(value)")) {
                ps.setString(1, FIRST_WEEK);
                ps.setLong(2, week);
                ps.executeUpdate();
            }
            return null;
        });
        synchronized (DecayService.class) {
            cachedFirstWeek = (long) week;
            loaded = true;
        }
    }

    /**
     * Charges every overdue weekly cutoff to a locked wallet row.
     *
     * @return the wallet balance after decay
     */
    static int settleIn(Connection c, long user, int wallet, int lastWeek, long now)
            throws SQLException {
        Long first = getFirstWeek(c);
        int current = weekIndex(now);
        if (first == null || current < first || current <= lastWeek) return wallet;

        int reserved = EconomyService.reservedTotal(c, user);
        int balance = wallet;
        int dueAdded = 0;
        for (int week = (int) Math.max(lastWeek + 1, first); week <= current; week++) {
            DecayMath.Step step = DecayMath.step(balance, reserved);
            dueAdded += step.shortfall;
            if (step.charged > 0) {
                balance = EconomyService.applyDelta(c, user, balance, -step.charged, "decay", "week:" + week,
                        "decay:" + user + ":" + week);
            }
        }
        try (PreparedStatement ps = c.prepareStatement("UPDATE economy SET last_decay_week = ?, "
                + "decay_due = decay_due + ? WHERE user = ?")) {
            ps.setInt(1, current);
            ps.setInt(2, dueAdded);
            ps.setLong(3, user);
            ps.executeUpdate();
        }
        return balance;
    }

    /**
     * Collects decay owed while coins were reserved, up to the amount released.
     */
    static void collectDueIn(Connection c, long user, int released, String reservationId) throws SQLException {
        int due;
        int wallet;
        try (PreparedStatement ps = c.prepareStatement("SELECT decay_due, wallet FROM economy WHERE user = ?")) {
            ps.setLong(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                due = rs.getInt(1);
                wallet = rs.getInt(2);
            }
        }
        int take = Math.min(Math.min(due, released), wallet);
        if (take <= 0) return;
        EconomyService.applyDelta(c, user, wallet, -take, "decay:due", reservationId, "decay-due:" + reservationId);
        try (PreparedStatement ps = c.prepareStatement("UPDATE economy SET decay_due = decay_due - ? WHERE user = ?")) {
            ps.setInt(1, take);
            ps.setLong(2, user);
            ps.executeUpdate();
        }
    }

    /**
     * Settles overdue weeks for every wallet above the protected amount.
     *
     * @return the number of wallets updated
     */
    public static int sweep() throws SQLException {
        Long first = getFirstWeek();
        int current = weekIndex(System.currentTimeMillis());
        if (first == null || current < first) return 0;

        List<Long> users = Database.inTransaction(c -> {
            List<Long> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT user FROM economy WHERE wallet > ? "
                    + "AND last_decay_week < ?")) {
                ps.setInt(1, DecayMath.PROTECTED);
                ps.setInt(2, current);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(rs.getLong(1));
                }
            }
            return list;
        });
        for (long user : users) {
            EconomyService.getInstance().getBalance(user); // locking a wallet settles it
        }
        return users.size();
    }

    /**
     * @return pending charge at the next cutoff for display, in whole GC
     */
    public static int estimateNext(long user) throws SQLException {
        return Database.inTransaction(c -> {
            EconomyService.lockWallet(c, user);
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT wallet FROM economy WHERE user = ?")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return DecayMath.estimateNext(rs.getInt(1));
                }
            }
        });
    }
}

package economy;

import database.Database;

/**
 * The one way every animated game takes and settles a stake, and the one place that rate limits them: each player
 * gets one game every few seconds, however many commands or buttons they use. Stakes themselves are not capped.
 */
public final class Wagers {

    private static volatile long cooldownMs = 3000;
    private static final java.util.Map<Long, Long> LAST = new java.util.concurrent.ConcurrentHashMap<>();

    private Wagers() { }

    /** Changes the pause between games; 0 turns the rate limit off (used by tests). */
    public static void setCooldownMs(long ms) {
        cooldownMs = ms;
        LAST.clear();
    }

    /**
     * Call when a player is about to start a game. Starting one uses up the pause.
     *
     * @return null if they may play now, otherwise a message saying how long to wait
     */
    public static String cooldown(long user) {
        long now = System.currentTimeMillis();
        long wait = 0;
        synchronized (LAST) {
            Long last = LAST.get(user);
            if (last != null) wait = last + cooldownMs - now;
            if (wait <= 0) LAST.put(user, now);
        }
        if (wait <= 0) return null;
        return "Easy there: you can start your next game in " + (wait + 999) / 1000 + "s.";
    }

    /** The cooldown, then the paid-play and balance checks. @return null to go ahead, otherwise the message to send */
    public static String gate(long user, int stake) throws java.sql.SQLException {
        String wait = cooldown(user);
        return wait != null ? wait : blocked(user, stake);
    }

    /** @return null if the player may stake this amount now, otherwise the message to send them */
    public static String blocked(long user, int stake) throws java.sql.SQLException {
        String blocked = WagerLimits.check(user, stake);
        if (blocked != null) return blocked;
        if (stake > EconomyService.getInstance().getSpendable(user)) return "You do not have " + stake + " GC spendable.";
        return null;
    }

    /**
     * Holds the stake and settles it in one transaction, so a crash cannot leave the stake held.
     *
     * @param net the player's result: positive to win that many coins, negative (at most the stake) to lose them
     * @throws EconomyService.InsufficientFundsRuntime if the player cannot cover the stake
     */
    public static void settleNow(long user, int stake, int net, String messageId, String reason) throws Exception {
        String reservation = reason + ":" + messageId;
        Database.inTransaction(c -> {
            EconomyService.reserveIn(c, user, stake, reason, messageId, reservation);
            EconomyService.settleReservationIn(c, reservation, net, reason);
            return null;
        });
    }
}

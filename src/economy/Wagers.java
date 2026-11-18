package economy;

import database.Database;

/** The one way every single-step paid game takes and settles a stake. */
public final class Wagers {

    private Wagers() { }

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

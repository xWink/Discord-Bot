package duel;

/**
 * Rating and pot arithmetic for duels. Stake size never changes a rating.
 */
public final class DuelMath {

    public static final int K = 32;
    public static final int START_RATING = 1000;
    /** Stakes at or above this pay the house fee. */
    public static final int FEE_THRESHOLD = 5;
    public static final int FEE = 1;
    public static final int RATED_MATCHES_FOR_RANK = 5;
    /** Ranked matches allowed between the same pair per day. */
    public static final int RANKED_PAIR_DAILY_CAP = 3;

    private DuelMath() { }

    /** 1 GC off the pot for stakes of 5 GC or more. */
    public static int fee(int stake) {
        return stake >= FEE_THRESHOLD ? FEE : 0;
    }

    /** What the winner gains: the other player's stake minus the fee. */
    public static int winnerNet(int stake) {
        return stake - fee(stake);
    }

    public static double expected(int ra, int rb) {
        return 1.0 / (1.0 + Math.pow(10, (rb - ra) / 400.0));
    }

    /** @return {newA, newB} after a decisive match */
    public static int[] update(int ra, int rb, boolean aWon) {
        double ea = expected(ra, rb);
        int a = (int) Math.round(ra + K * ((aWon ? 1 : 0) - ea));
        int b = (int) Math.round(rb + K * ((aWon ? 0 : 1) - (1 - ea)));
        return new int[]{a, b};
    }
}

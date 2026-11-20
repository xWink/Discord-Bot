package command.util.game;

import java.util.Random;

/**
 * Three reels, six symbols. Three alike pay by symbol; exactly two alike pay 1.4x; anything else loses the stake.
 * Payouts are in tenths of the stake and rounded down to a whole coin, so the return to the player is about 95%.
 */
public final class SlotsGame {

    /** Reel symbols, rarest last. {@code weight} is out of 100 and {@code tripleTenths} is the payout in tenths. */
    public enum Symbol {
        CHERRY(30, 30), LEMON(25, 50), BELL(20, 80), STAR(12, 150), SEVEN(8, 250), MASCOT(5, 500);

        final int weight;
        final int tripleTenths;

        Symbol(int weight, int tripleTenths) {
            this.weight = weight;
            this.tripleTenths = tripleTenths;
        }

        public int weight() { return weight; }

        /** @return what three of this symbol pay, as a multiple of the stake */
        public double tripleMultiplier() { return tripleTenths / 10.0; }
    }

    /** What exactly two alike pay, in tenths of the stake. */
    public static final int PAIR_TENTHS = 14;

    private SlotsGame() { }

    public static Symbol[] spin(Random random) {
        return new Symbol[]{draw(random), draw(random), draw(random)};
    }

    private static Symbol draw(Random random) {
        int roll = random.nextInt(100);
        for (Symbol s : Symbol.values()) {
            roll -= s.weight;
            if (roll < 0) return s;
        }
        return Symbol.CHERRY;
    }

    /** @return the coins paid back for the stake (0 for a loss), before taking the stake off */
    public static int payout(Symbol[] reels, int stake) {
        if (reels[0] == reels[1] && reels[1] == reels[2]) return (int) ((long) stake * reels[0].tripleTenths / 10);
        boolean pair = reels[0] == reels[1] || reels[1] == reels[2] || reels[0] == reels[2];
        return pair ? (int) ((long) stake * PAIR_TENTHS / 10) : 0;
    }

    /** @return the player's net result: payout minus stake */
    public static int net(Symbol[] reels, int stake) {
        return payout(reels, stake) - stake;
    }

    /** @return a short label for the result line, for example {@code "THREE STARS x15"} */
    public static String label(Symbol[] reels) {
        if (reels[0] == reels[1] && reels[1] == reels[2]) {
            return "THREE " + reels[0].name() + "S x" + trim(reels[0].tripleMultiplier());
        }
        boolean pair = reels[0] == reels[1] || reels[1] == reels[2] || reels[0] == reels[2];
        return pair ? "PAIR x" + trim(PAIR_TENTHS / 10.0) : "NO MATCH";
    }

    private static String trim(double d) {
        return d == Math.rint(d) ? Long.toString((long) d) : Double.toString(d);
    }
}

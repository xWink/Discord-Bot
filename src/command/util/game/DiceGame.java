package command.util.game;

import java.util.Random;

/**
 * Two-dice prediction. Published odds: low (2-6) and high (8-12) each win
 * 15/36 and pay 1:1; exactly seven wins 6/36 and pays 4:1. A miss forfeits the
 * stake. Free play uses the same rules with no stake.
 */
public final class DiceGame {

    public enum Pick {
        LOW(1), HIGH(1), SEVEN(4);

        private final int multiplier;

        Pick(int multiplier) {
            this.multiplier = multiplier;
        }

        public int getMultiplier() {
            return multiplier;
        }

        /** Parses a user's word, or returns null. */
        public static Pick parse(String text) {
            for (Pick p : values()) {
                if (p.name().equalsIgnoreCase(text)) return p;
            }
            return null;
        }
    }

    private DiceGame() { }

    public static int[] roll(Random random) {
        return new int[]{random.nextInt(6) + 1, random.nextInt(6) + 1};
    }

    public static boolean wins(Pick pick, int total) {
        switch (pick) {
            case LOW: return total <= 6;
            case HIGH: return total >= 8;
            default: return total == 7;
        }
    }

    /**
     * @return change to the wallet: the multiplied stake on a win, minus the
     *         stake on a miss
     */
    public static int net(Pick pick, int total, int stake) {
        return wins(pick, total) ? stake * pick.getMultiplier() : -stake;
    }
}

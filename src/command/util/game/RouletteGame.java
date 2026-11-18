package command.util.game;

import java.util.Random;

/**
 * European roulette: one zero, 36 numbers. Colour, odd or even and low or high pay 1:1 and lose on zero; a single
 * number (zero included) pays 35:1. The house edge is 1/37, about 2.7%.
 */
public final class RouletteGame {

    /** The pockets in wheel order, clockwise from zero. */
    public static final int[] WHEEL = {0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10, 5, 24, 16,
            33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26};

    private static final java.util.Set<Integer> REDS = new java.util.HashSet<>(java.util.Arrays.asList(
            1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36));

    private RouletteGame() { }

    /** What a player can bet on. */
    public static final class Bet {
        public enum Kind { RED, BLACK, ODD, EVEN, LOW, HIGH, NUMBER }

        public final Kind kind;
        public final int number;

        Bet(Kind kind, int number) {
            this.kind = kind;
            this.number = number;
        }

        public String describe() {
            return kind == Kind.NUMBER ? "number " + number : kind.name().toLowerCase();
        }

        /** @return true if this bet wins when the ball lands on {@code result} */
        public boolean wins(int result) {
            switch (kind) {
                case RED: return isRed(result);
                case BLACK: return result != 0 && !isRed(result);
                case ODD: return result != 0 && result % 2 == 1;
                case EVEN: return result != 0 && result % 2 == 0;
                case LOW: return result >= 1 && result <= 18;
                case HIGH: return result >= 19;
                default: return result == number;
            }
        }

        /** @return how many times the stake a win pays */
        public int payout() {
            return kind == Kind.NUMBER ? 35 : 1;
        }
    }

    /** @return the bet, or null if the text is not one ({@code red}, {@code odd}, {@code 17}, {@code green}...) */
    public static Bet parse(String text) {
        String t = text.toLowerCase();
        switch (t) {
            case "red": return new Bet(Bet.Kind.RED, -1);
            case "black": return new Bet(Bet.Kind.BLACK, -1);
            case "odd": return new Bet(Bet.Kind.ODD, -1);
            case "even": return new Bet(Bet.Kind.EVEN, -1);
            case "low": return new Bet(Bet.Kind.LOW, -1);
            case "high": return new Bet(Bet.Kind.HIGH, -1);
            case "green": return new Bet(Bet.Kind.NUMBER, 0);
            default:
                try {
                    int n = Integer.parseInt(t);
                    return n >= 0 && n <= 36 ? new Bet(Bet.Kind.NUMBER, n) : null;
                } catch (NumberFormatException e) {
                    return null;
                }
        }
    }

    public static boolean isRed(int n) {
        return REDS.contains(n);
    }

    public static int spin(Random random) {
        return random.nextInt(37);
    }

    /** @return the position of a number on the wheel, 0 to 36 */
    public static int pocket(int number) {
        for (int i = 0; i < WHEEL.length; i++) if (WHEEL[i] == number) return i;
        throw new IllegalArgumentException("Not a pocket: " + number);
    }

    /** @return the player's net result for the stake */
    public static int net(Bet bet, int result, int stake) {
        return bet.wins(result) ? stake * bet.payout() : -stake;
    }
}

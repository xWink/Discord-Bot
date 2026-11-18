package command.util.game;

import java.util.Random;

/**
 * Crash. A multiplier climbs from 1.00 and crashes at a random point. The player names a target in advance; if the
 * crash point is at or above it they cash out at the target and win, otherwise they lose the stake. The chance to
 * reach a target is 97 / target, so every target returns about 97%.
 * Multipliers are kept in hundredths (250 means 2.50x) so the money maths is exact.
 */
public final class CrashGame {

    public static final int MIN_TARGET = 110;
    public static final int MAX_TARGET = 100_000;
    public static final int DEFAULT_TARGET = 200;

    private CrashGame() { }

    /** @return the crash point in hundredths, at least 100 (an instant crash) */
    public static int crashPoint(Random random) {
        double r = random.nextDouble();
        double point = Math.floor(97.0 / (1 - r));
        return (int) Math.max(100, Math.min(point, 10_000_000));
    }

    public static boolean wins(int crashPoint, int target) {
        return crashPoint >= target;
    }

    /** @return the player's net result: the stake times (target - 1) on a win, minus the stake on a loss */
    public static int net(int crashPoint, int target, int stake) {
        return wins(crashPoint, target) ? (int) ((long) stake * (target - 100) / 100) : -stake;
    }

    /** @return the target in hundredths, or -1 if the text is not a number from 1.1 to 1000 */
    public static int parseTarget(String text) {
        try {
            double d = Double.parseDouble(text.toLowerCase().replace("x", ""));
            int hundredths = (int) Math.round(d * 100);
            return hundredths >= MIN_TARGET && hundredths <= MAX_TARGET ? hundredths : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String show(int hundredths) {
        return String.format("%d.%02dx", hundredths / 100, hundredths % 100);
    }
}

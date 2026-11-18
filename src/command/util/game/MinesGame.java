package command.util.game;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Mines. A 4x4 board hides some mines. Each safe tile you uncover raises the payout; hit a mine and the stake is
 * lost. You can cash out after any safe tile. The payout after k safe tiles is 97% of the odds of surviving that
 * long, so the game returns about 97%.
 */
public class MinesGame {

    public static final int TILES = 16;
    public static final int MIN_MINES = 1;
    public static final int MAX_MINES = 8;
    public static final int DEFAULT_MINES = 3;

    public enum Result { SAFE, MINE, ALREADY }

    private final long userId;
    private final int stake;
    private final String reservationId;
    private final Set<Integer> mines = new HashSet<>();
    private final Set<Integer> found = new HashSet<>();
    private int exploded = -1;
    private long lastAction = System.currentTimeMillis();

    public MinesGame(long userId, int stake, String reservationId, int mineCount, Random random) {
        this.userId = userId;
        this.stake = stake;
        this.reservationId = reservationId;
        while (mines.size() < mineCount) mines.add(random.nextInt(TILES));
    }

    /** @param tile 0 to 15 */
    public Result pick(int tile) {
        lastAction = System.currentTimeMillis();
        if (found.contains(tile) || exploded >= 0) return Result.ALREADY;
        if (mines.contains(tile)) {
            exploded = tile;
            return Result.MINE;
        }
        found.add(tile);
        return Result.SAFE;
    }

    /** @return the payout multiple after {@code safe} safe tiles with {@code mineCount} mines */
    public static double multiplier(int mineCount, int safe) {
        double m = 0.97;
        for (int i = 0; i < safe; i++) m *= (double) (TILES - i) / (TILES - mineCount - i);
        return safe == 0 ? 1.0 : m;
    }

    public double multiplier() {
        return multiplier(mines.size(), found.size());
    }

    /** @return the net gain if the player stops now (never negative: stopping after a safe tile is a gain or push) */
    public int netIfStopped() {
        return (int) Math.floor(stake * multiplier()) - stake;
    }

    public boolean allSafeFound() {
        return found.size() == TILES - mines.size();
    }

    public long getUserId() { return userId; }
    public int getStake() { return stake; }
    public String getReservationId() { return reservationId; }
    public Set<Integer> getMines() { return mines; }
    public Set<Integer> getFound() { return found; }
    public int getExploded() { return exploded; }
    public long getLastAction() { return lastAction; }
}

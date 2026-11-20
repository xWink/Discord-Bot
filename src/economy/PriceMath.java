package economy;

/**
 * Weekly price moves for featured shop items. Prices stay within 80-120% of
 * base and move by at most 10% of base a week. Small samples hold the price.
 */
public final class PriceMath {

    public static final int MIN_SAMPLE = 5;        // distinct buyers across all featured items
    public static final int TARGET_BUYERS = 3;     // distinct buyers per item per week
    public static final double STEP = 0.10;
    public static final double FLOOR = 0.80;
    public static final double CEILING = 1.20;

    private PriceMath() { }

    /**
     * @param base       the item's base price
     * @param current    this week's price
     * @param buyers     distinct buyers of this item last week
     * @param sampleSize distinct buyers across every featured item last week
     * @return next week's price in whole GC
     */
    public static int next(int base, int current, int buyers, int sampleSize) {
        int min = (int) Math.ceil(base * FLOOR);
        int max = (int) Math.floor(base * CEILING);
        if (sampleSize < MIN_SAMPLE) return clamp(current, min, max);
        int move = (int) Math.max(1, Math.round(base * STEP));
        int price = current;
        if (buyers * 2 > TARGET_BUYERS * 3) price += move;        // well above target: raise modestly
        else if (buyers * 2 < TARGET_BUYERS) price -= move;       // weak demand: drop
        return clamp(price, min, max);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

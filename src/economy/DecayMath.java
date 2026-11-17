package economy;

/**
 * Pure arithmetic for weekly balance decay: 2% a week on the part of a wallet
 * above 100 GC. Fractions are carried in thousandths of a GC so nothing is
 * lost to rounding.
 */
public final class DecayMath {

    /** Part of a wallet that never decays. */
    public static final int PROTECTED = 100;

    /** Weekly rate in thousandths of a GC per GC of excess (2% = 20). */
    public static final int RATE_MILLI_PER_GC = 20;

    private DecayMath() { }

    /** Outcome of one weekly cutoff. */
    public static final class Step {
        /** Whole GC actually removed from the wallet. */
        public final int charged;
        /** Whole GC owed but not collectable now because the coins are reserved. */
        public final int shortfall;
        /** Fraction carried to the next week, in thousandths of a GC. */
        public final long carryMilli;

        Step(int charged, int shortfall, long carryMilli) {
            this.charged = charged;
            this.shortfall = shortfall;
            this.carryMilli = carryMilli;
        }
    }

    /**
     * Reserved stakes count toward the balance being assessed, but the
     * deduction is capped at the unreserved coins. The rest becomes a debt
     * collected when the reservation is released.
     */
    public static Step step(int balance, long carryMilli, int reserved) {
        long excess = Math.max(0, balance - PROTECTED);
        long total = carryMilli + excess * RATE_MILLI_PER_GC;
        int whole = (int) (total / 1000);
        int unreserved = Math.max(0, balance - reserved);
        int charged = Math.min(whole, unreserved);
        return new Step(charged, whole - charged, total % 1000);
    }

    /**
     * @return the GC (with fraction) the next cutoff would take from this wallet
     */
    public static double estimateNext(int balance, long carryMilli) {
        long excess = Math.max(0, balance - PROTECTED);
        return (carryMilli + excess * RATE_MILLI_PER_GC) / 1000.0;
    }
}

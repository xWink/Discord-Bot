package economy;

/**
 * Pure arithmetic for weekly balance decay: 2% a week on the part of a wallet
 * above 100 GC, rounded down to whole GC. Balances are always whole numbers, so
 * a small excess simply pays nothing that week (under 50 GC above the line is
 * less than one coin).
 */
public final class DecayMath {

    /** Part of a wallet that never decays. */
    public static final int PROTECTED = 100;

    /** Weekly rate in percent of the excess. */
    public static final int RATE_PERCENT = 2;

    private DecayMath() { }

    /** Outcome of one weekly cutoff. */
    public static final class Step {
        /** Whole GC actually removed from the wallet. */
        public final int charged;
        /** Whole GC owed but not collectable now because the coins are reserved. */
        public final int shortfall;

        Step(int charged, int shortfall) {
            this.charged = charged;
            this.shortfall = shortfall;
        }
    }

    /**
     * @return the whole GC a wallet owes for one week: 2% of the part above the protected amount, rounded down
     */
    public static int owed(int balance) {
        long excess = Math.max(0, balance - PROTECTED);
        return (int) (excess * RATE_PERCENT / 100);
    }

    /**
     * Reserved stakes count toward the balance being assessed, but the
     * deduction is capped at the unreserved coins. The rest becomes a debt
     * collected when the reservation is released.
     */
    public static Step step(int balance, int reserved) {
        int whole = owed(balance);
        int unreserved = Math.max(0, balance - reserved);
        int charged = Math.min(whole, unreserved);
        return new Step(charged, whole - charged);
    }

    /**
     * @return the whole GC the next cutoff would take from this wallet
     */
    public static int estimateNext(int balance) {
        return owed(balance);
    }
}

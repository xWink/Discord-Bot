package economy;

/**
 * Thrown when a debit, transfer, or reservation exceeds the spendable balance.
 */
public class InsufficientFundsException extends Exception {

    private final int spendable;

    public InsufficientFundsException(int spendable, int needed) {
        super("Spendable balance " + spendable + " is below the required " + needed);
        this.spendable = spendable;
    }

    /**
     * @return the spendable balance at the time of the failed operation
     */
    public int getSpendable() {
        return spendable;
    }
}

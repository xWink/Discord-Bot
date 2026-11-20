package economy;

/**
 * The quoted price is no longer the current price; the buyer must confirm again.
 */
public class PriceChangedException extends Exception {

    private final int newPrice;

    public PriceChangedException(int newPrice) {
        super("Price changed to " + newPrice);
        this.newPrice = newPrice;
    }

    public int getNewPrice() {
        return newPrice;
    }
}

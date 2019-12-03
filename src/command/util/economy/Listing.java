package command.util.economy;

public abstract class Listing {

    /**
     * The cost of the listed item.
     */
    private int cost;
    /**
     * The amount of time the item lasts after purchase.
     */
    private int duration;

    /**
     * Listing constructor. Every listing has a cost and duration
     * that must be initialized.
     *
     * @param theCost the listed item's cost
     * @param theDuration how long the item lasts after purchase (# of days)
     */
    public Listing(int theCost, int theDuration) {
        cost = theCost;
        duration = theDuration;
    }

    /**
     * Cost getter.
     *
     * @return the item's cost
     */
    public int getCost() {
        return cost;
    }

    /**
     * Duration getter.
     *
     * @return the item's duration after purchase
     */
    public int getDuration() {
        return duration;
    }

    /**
     * The string representation of the listing
     * for display on the marketplace.
     *
     * @return the string representation of the listed item
     */
    @Override
    public abstract String toString();
}

package command.util.game;

import command.util.cards.HandOfCards;

public class Player {

    private long userId;
    private HandOfCards hand;

    /**
     * Initializes player with default user ID of 0 and new hand.
     */
    public Player() {
        this(0, new HandOfCards());
    }

    /**
     * Initializes player with specified user ID and new hand.
     * @param theUserId the userId of the player
     */
    public Player(long theUserId) {
        this(theUserId, new HandOfCards());
    }

    /**
     * Initializes player with default use ID of 0 and specified hand.
     *
     * @param playerHand the player's hand of cards
     */
    public Player(HandOfCards playerHand) {
        this(0, playerHand);
    }

    /**
     * Initializes player with specified user ID and specified hand.
     *
     * @param theUserId the player's userId
     * @param playerHand the player's hand of cards
     */
    public Player(long theUserId, HandOfCards playerHand) {
        userId = theUserId;
        hand = playerHand;
    }

    /**
     * Getter for player's user ID.
     *
     * @return the player's user ID
     */
    public long getUserId() {
        return userId;
    }

    /**
     * Getter for player's hand.
     *
     * @return the player's hand of cards
     */
    public HandOfCards getHand() {
        return hand;
    }
}

package command.util.game;

import command.util.cards.DeckOfCards;

/**
 * One round of blackjack. Published rules: the dealer stands on 17 or more, a
 * natural blackjack pays 3:2 (rounded down), a push returns the stake, and a
 * player bust loses the stake immediately, whatever the dealer holds.
 */
public class BlackJackGame {

    private Player player;
    private Player dealer;
    private int betAmount;
    private DeckOfCards deck;
    private final String reservationId;
    private long channelId;
    private long lastAction;
    private boolean settled;

    /**
     * Initializes the game with a specified player and bet amount.
     *
     * @param thePlayer the player (user) in the game
     * @param theBet the amount the player bet to create the game
     */
    public BlackJackGame(Player thePlayer, int theBet) {
        this(thePlayer, new Player(), theBet);
    }

    /**
     * Initializes the game with a specified player, dealer, and bet amount.
     *
     * @param thePlayer the player (user) in the game
     * @param theDealer the dealer (cpu) in the game
     * @param theBet the amount the player bet to create the game
     */
    public BlackJackGame(Player thePlayer, Player theDealer, int theBet) {
        this(thePlayer, theDealer, theBet, null, null);
        deck = new DeckOfCards(5);
        deck.shuffle();
    }

    /**
     * Restores a persisted game.
     */
    public BlackJackGame(Player thePlayer, Player theDealer, int theBet, DeckOfCards theDeck,
                         String theReservationId) {
        player = thePlayer;
        dealer = theDealer;
        betAmount = theBet;
        deck = theDeck;
        reservationId = theReservationId != null ? theReservationId
                : "bj:" + thePlayer.getUserId() + ":" + System.nanoTime();
        lastAction = System.currentTimeMillis();
    }

    /**
     * Starts the game by handing the player and dealer 2 cards each.
     */
    public void start() {
        getPlayer().getHand().getAsList().clear();
        getDealer().getHand().getAsList().clear();

        getPlayer().getHand().add(deck.pickTopCard());
        getDealer().getHand().add(deck.pickTopCard());
        getPlayer().getHand().add(deck.pickTopCard());
        getDealer().getHand().add(deck.pickTopCard());
    }

    /**
     * Allows the player to hit by giving them a card.
     *
     * @return the new value of the player's hand after receiving a card
     */
    public int hit() {
        getPlayer().getHand().add(deck.pickTopCard());
        return getPlayer().getHand().getValue();
    }

    /**
     * Makes the dealer hit until their hand value is 17 or more.
     */
    public void dealerPlays() {
        if (getPlayer().getHand().getValue() > 21) return;
        if (getPlayer().getHand().getValue() == 21 && getPlayer().getHand().getAsList().size() == 2)
            return;
        while (getDealer().getHand().getValue() < 17)
            getDealer().getHand().add(deck.pickTopCard());
    }

    /**
     * Resolves the round and returns the change to the player's wallet
     * (negative if they lost). A player bust ends the round before the dealer
     * plays, so a double bust is a loss, not a tie.
     *
     * @return the player's earnings (losses if negative)
     */
    public int checkWinner() {
        int playerValue = getPlayer().getHand().getValue();
        if (playerValue > 21) return -betAmount;

        dealerPlays();
        int dealerValue = getDealer().getHand().getValue();

        boolean playerNatural = playerValue == 21 && player.getHand().getAsList().size() == 2;
        boolean dealerNatural = dealerValue == 21 && dealer.getHand().getAsList().size() == 2;

        if (playerNatural && !dealerNatural) return (int) (betAmount * 1.5);
        if (dealerNatural && !playerNatural) return -betAmount;
        if (dealerValue > 21) return betAmount;
        if (dealerValue == playerValue) return 0;
        return playerValue > dealerValue ? betAmount : -betAmount;
    }

    /**
     * Marks the game finished and clears its references.
     */
    public void end() {
        settled = true;
    }

    public Player getPlayer() {
        return player;
    }

    public Player getDealer() {
        return dealer;
    }

    public int getBet() {
        return betAmount;
    }

    public DeckOfCards getDeck() {
        return deck;
    }

    public String getReservationId() {
        return reservationId;
    }

    public long getChannelId() {
        return channelId;
    }

    public void setChannelId(long id) {
        channelId = id;
    }

    public long getLastAction() {
        return lastAction;
    }

    /** Records player activity for the inactivity timeout. */
    public void touch() {
        lastAction = System.currentTimeMillis();
    }

    public boolean isSettled() {
        return settled;
    }

    void markSettled() {
        settled = true;
    }
}

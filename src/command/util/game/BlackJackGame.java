package command.util.game;

import command.util.cards.DeckOfCards;

public class BlackJackGame {

    private Player player;
    private Player dealer;
    private int betAmount;
    private DeckOfCards deck;

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
        player = thePlayer;
        dealer = theDealer;
        deck = new DeckOfCards(5);
        deck.shuffle();
        betAmount = theBet;
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
     * Makes the dealer hit until their hand value is over 17.
     */
    public void dealerPlays() {
        while (getDealer().getHand().getValue() < 17)
            getDealer().getHand().add(deck.pickTopCard());
    }

    /**
     * Checks whether the player or dealer won or if there was a tie then returns the
     * amount the player won (negative if they lost money).
     *
     * @return the player's earnings (losses if negative)
     */
    public int checkWinner() {
        dealerPlays();
        int playerValue = getPlayer().getHand().getValue();
        int dealerValue = getDealer().getHand().getValue();

        if (dealerValue == playerValue || (playerValue > 21 && dealerValue > 21)) return 0; // Tie
        if (playerValue == 21) return (int) (betAmount * 1.5); // Player won with 21
        if (playerValue > 21) return -betAmount; // Player busted
        if (dealerValue > 21) return betAmount; // Dealer busted
        return playerValue > dealerValue ? betAmount : -betAmount; // Higher value wins
    }

    /**
     * Sets the player and dealer to null and the betAmount to 0.
     */
    public void end() {
        player = null;
        dealer = null;
        betAmount = 0;
    }

    /**
     * Player getter.
     *
     * @return the player of the game
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Dealer getter.
     *
     * @return the dealer for the game
     */
    public Player getDealer() {
        return dealer;
    }
}

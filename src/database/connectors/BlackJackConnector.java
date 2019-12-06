package database.connectors;

import command.util.cards.Card;
import command.util.cards.DeckOfCards;
import database.Connector;

public final class BlackJackConnector extends Connector {

    /**
     * Initializes the table to "blackjack".
     */
    public BlackJackConnector() {
        super("blackjack");
    }

    /**
     * Adds a new user to a specified ta the blackjack table based on their ID,
     * bet amount, and first two cards.
     *
     * @param userId the ID number of the new user being added
     * @param betAmount the amount the user bet for the game
     * @param firstCard the first card the user started with
     * @param secondCard the second card the user started with
     */
    public void startGame(long userId, int betAmount, Card firstCard, Card secondCard) {
        try {
            getConnection().prepareStatement("INSERT INTO blackjack (user, bet, card1, card2) VALUES "
                            + "(" + userId + ", " + betAmount + ", '" + firstCard.toDbFormat()
                            + "', '" + secondCard.toDbFormat() + "')").executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Searches the blackjack table for a row with a matching user ID and returns
     * whether or not such a row was found.
     *
     * @param userId the id number of the Discord user being searched for
     * @return true if found, false if not found or error occurs
     */
    public boolean userExists(long userId) {
        return super.userExists(userId, getTable());
    }

    /**
     * Adds a new user to the database with a random pair of cards and a starting bet of 0.
     *
     * @param userId the ID number of the new user being added
     */
    @Override
    protected void addUser(long userId) {
        try {
            getConnection().prepareStatement("INSERT INTO blackjack (user, bet, card1, card2) VALUES "
                    + "(" + userId + ", " + 0 + ", '" + new DeckOfCards(1).pickRandomCard()
                    + "', '" + new DeckOfCards(1).pickRandomCard() + "')").executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

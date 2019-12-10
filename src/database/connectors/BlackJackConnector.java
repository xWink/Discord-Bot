package database.connectors;

import command.util.cards.Card;
import command.util.cards.CardRank;
import command.util.cards.CardSuit;
import command.util.cards.DeckOfCards;
import command.util.cards.HandOfCards;
import database.Connector;

import java.sql.ResultSet;
import java.sql.SQLException;

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
     * Returns the user's hand from the table based on user ID.
     *
     * @param userId the user's ID
     * @return the HandOfCards in the user's row
     * @throws SQLException may be thrown when making a prepared statement
     */
    public HandOfCards getHand(long userId) throws SQLException {
        int i = 1;
        String cardString;
        ResultSet rs = getUserRow(userId);
        HandOfCards hand = new HandOfCards();

        // For each card in the player's hand
        while (rs != null && (cardString = rs.getString("card" + (i++))) != null) {
            String rankString = cardString.split(" ")[0];
            String suitString = cardString.split(" ")[1];

            CardRank rank = CardRank.getCardRank(rankString); // Make rank object
            CardSuit suit = CardSuit.getCardSuit(suitString); // Make suit object

            hand.add(new Card(rank, suit));
        }

        return hand;
    }

    /**
     * Finds the first empty column in the user's row in the database
     * and adds the specified card to that column.
     *
     * @param userId the user's ID number
     * @param card the card being added to the user's row
     * @throws SQLException may be thrown when making a prepared statement
     */
    public void addCard(long userId, Card card) throws SQLException {
        int index = 1;

        while (getUserRow(userId).getString("card" + index) != null) {
            index++;
        }

        getConnection().prepareStatement("UPDATE blackjack SET card" + index + " = '"
                + card.toDbFormat() + "' WHERE user = " + userId).executeUpdate();
    }

    /**
     * Returns a user's row in the table.
     *
     * @param userId the user's ID number
     * @return a ResultSet containing the user's entire row in blackjack
     */
    public ResultSet getUserRow(long userId) {
        ResultSet rs = getUserRow(userId, getTable());
//        try {
//            rs.next();
//        } catch (SQLException ignored) { }
        return rs;
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

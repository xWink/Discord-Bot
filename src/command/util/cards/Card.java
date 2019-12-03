package command.util.cards;

public class Card {

    /**
     * The card's rank.
     */
    private final CardRank rank;
    /**
     * The card's suit.
     */
    private final CardSuit suit;

    /**
     * Initializes a new Card with a specified rank and suit.
     *
     * @param theRank the card's rank
     * @param theSuit the card's suit
     */
    public Card(CardRank theRank, CardSuit theSuit) {
        rank = theRank;
        suit = theSuit;
    }

    /**
     * Rank getter.
     *
     * @return the card's rank
     */
    public CardRank getRank() {
        return rank;
    }

    /**
     * Suit getter.
     *
     * @return the card's suit
     */
    public CardSuit getSuit() {
        return suit;
    }

    /**
     * Getter for the card's data as a string in the format:
     * rank's symbol + " of " + suit's name.
     *
     * @return the string representation of the card
     */
    @Override
    public String toString() {
        return rank.getSymbol() + " of " + suit.getName();
    }

    /**
     * Getter for the card in emote format meant for use
     * in Discord messaging channels.
     *
     * @return the card represented in a compact emote format
     */
    public String toEmote() {
        return "[" + rank.getSymbol() + " " + suit.getEmote() + "]";
    }

    /**
     * Getter for the string representation of the card intended
     * for use in the database.
     *
     * @return the card represented in a format meant for use in
     * the database
     */
    public String toDbFormat() {
        return rank.getSymbol() + " " + suit.getInitial();
    }
}

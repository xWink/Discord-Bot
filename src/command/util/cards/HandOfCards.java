package command.util.cards;

import java.util.ArrayList;
import java.util.List;

public class HandOfCards {

    private ArrayList<Card> hand;

    /**
     * Initializes the hand as an empty ArrayList of Cards.
     */
    public HandOfCards() {
        reset();
    }

    /**
     * Initializes the hand with a list of specified cards.
     *
     * @param cards the cards the hand is initialized with
     */
    public HandOfCards(List<Card> cards) {
        this();
        add(cards);
    }

    /**
     * Resets the hand to an empty ArrayList.
     */
    private void reset() {
        hand = new ArrayList<>();
    }

    /**
     * Adds a specified card to the hand.
     *
     * @param card the card being added
     */
    public void add(Card card) {
        hand.add(card);
    }

    /**
     * Adds a list of specified cards to the hand.
     *
     * @param cards the cards being added
     */
    public void add(List<Card> cards) {
        hand.addAll(cards);
    }

    /**
     * Removes a specified card from the hand.
     *
     * @param card the card being removed
     */
    public void remove(Card card) {
        hand.remove(card);
    }

    /**
     * Hand getter.
     *
     * @return the ArrayList of cards that is the hand
     */
    public ArrayList<Card> getHand() {
        return hand;
    }

    /**
     * Getter for the total numerical value of the hand.
     *
     * @return the sum of the values of every card in the hand
     */
    public int getValue() {
        int value = 0;
        int numAces = 0;

        for (Card card : hand) {
            if (card.getRank().equals(CardRank.ACE)) {
                numAces++;
            }
            value += card.getRank().getValue();
        }
        while (numAces > 0 && value > 21) {
            numAces--;
            value -= 10;
        }
        return value;
    }

    /**
     * Shows the full contents of the hand in string format.
     * Meant for use in conjunction with Discord emotes.
     *
     * @return the string containing information on every card
     * in the hand
     */
    @Override
    public String toString() {
        StringBuilder handString = new StringBuilder();
        for (Card card : hand) {
            handString.append(card.toEmote()).append(" ");
        }
        return handString.toString();
    }
}

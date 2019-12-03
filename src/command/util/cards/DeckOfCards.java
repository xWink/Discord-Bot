package command.util.cards;

import java.util.ArrayList;
import java.util.Collections;

public class DeckOfCards {

    /**
     * ArrayList of all cards in the deck.
     */
    private ArrayList<Card> deck;

    /**
     * Initializes the deck of cards
     * with a specified number of decks.
     *
     * Passing 0 will start with an empty deck,
     * 1 will start with a standard 52 card deck,
     * and any greater number will start with multiple
     * decks put together.
     *
     * @param numDecks the number of decks to start with
     */
    public DeckOfCards(int numDecks) {
        deck = new ArrayList<>();
        addDecks(numDecks);
    }

    /**
     * Adds a specified number of whole decks
     * onto the deck. Each deck contains the
     * standard 52 cards of a playing card deck.
     *
     * @param numDecks the number of decks to be added
     */
    public void addDecks(int numDecks) {
        for (int i = 0; i < numDecks; i++) {
            for (CardSuit suit : CardSuit.values()) {
                for (CardRank rank : CardRank.values()) {
                    deck.add(new Card(rank, suit));
                }
            }
        }
    }

    /**
     * Shuffles the deck.
     */
    public void shuffle() {
        Collections.shuffle(deck);
    }

    /**
     * Adds a specified card to the bottom of the deck.
     *
     * @param card the card being added
     */
    public void addCardToBottom(Card card) {
        deck.add(card);
    }

    /**
     * Adds a specified card to the top of the deck.
     *
     * @param card the card being added
     */
    public void addCardToTop(Card card) {
        deck.add(0, card);
    }

    /**
     * Adds a specified card to a random spot
     * in the deck.
     *
     * @param card the card being added
     */
    public void addCardRandomly(Card card) {
        deck.add((int) (Math.random() * deck.size()), card);
    }

    /**
     * Gets the card at the top of the deck
     * and removes that card from the deck.
     *
     * @return the first card in the deck
     */
    public Card pickTopCard() {
        Card card = deck.get(0);
        deck.remove(0);
        return card;
    }

    /**
     * Gets the card at the bottom of the deck
     * and removes that card from the deck.
     *
     * @return the last card in the deck
     */
    public Card pickBottomCard() {
        Card card = deck.get(deck.size() - 1);
        deck.remove(deck.size() - 1);
        return card;
    }

    /**
     * Gets a random card from the deck
     * and removes that card from the deck.
     *
     * @return the randomly chosen card in the deck
     */
    public Card pickRandomCard() {
        int index = (int) (Math.random() * deck.size());
        Card card = deck.get(index);
        deck.remove(index);
        return card;
    }

    /**
     * Shows a card at a specified index in the deck
     * without removing it.
     *
     * @param index the index of the card being revealed
     * @return the card that was revealed at index
     */
    public Card showCard(int index) {
        return deck.get(index);
    }

    /**
     * Getter for the size of the deck.
     *
     * @return the number of cards in the deck
     */
    public int getSize() {
        return deck.size();
    }
}

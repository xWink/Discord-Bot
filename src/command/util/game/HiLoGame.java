package command.util.game;

import command.util.cards.Card;
import command.util.cards.DeckOfCards;

/**
 * Higher or Lower. One card is shown; guess whether the next is higher or
 * lower (ace high). A tie loses. A correct first guess is worth the stake and
 * can be cashed out; a correct second guess is worth twice the stake. Any
 * wrong guess forfeits the stake.
 */
public class HiLoGame {

    public static final int MAX_GUESSES = 2;

    private final long userId;
    private final int stake;
    private final String reservationId;
    private final DeckOfCards deck;
    private Card current;
    private int correct;
    private long lastAction = System.currentTimeMillis();

    public HiLoGame(long userId, int stake, String reservationId, DeckOfCards deck) {
        this.userId = userId;
        this.stake = stake;
        this.reservationId = reservationId;
        this.deck = deck;
        this.current = deck.pickTopCard();
    }

    public static int rankValue(Card card) {
        return card.getRank().ordinal() + 2; // 2..14, ace high
    }

    /**
     * @return true if the guess was right; the new card becomes current
     */
    public boolean guess(boolean higher) {
        Card next = deck.pickTopCard();
        int a = rankValue(current);
        int b = rankValue(next);
        current = next;
        touch();
        boolean ok = higher ? b > a : b < a;
        if (ok) correct++;
        return ok;
    }

    /** Net change to the wallet if the player stops now. */
    public int netIfStopped() {
        return stake * correct;
    }

    public boolean isFinished() {
        return correct >= MAX_GUESSES;
    }

    public long getUserId() { return userId; }
    public int getStake() { return stake; }
    public String getReservationId() { return reservationId; }
    public Card getCurrent() { return current; }
    public int getCorrect() { return correct; }
    public long getLastAction() { return lastAction; }
    public void touch() { lastAction = System.currentTimeMillis(); }

}

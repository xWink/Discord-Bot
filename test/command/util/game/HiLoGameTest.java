package command.util.game;

import command.util.cards.Card;
import command.util.cards.CardRank;
import command.util.cards.CardSuit;
import command.util.cards.DeckOfCards;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class HiLoGameTest {

    private static Card c(CardRank r) { return new Card(r, CardSuit.SPADES); }

    private static HiLoGame game(Card... cards) {
        return new HiLoGame(1, 5, "r", DeckOfCards.of(Arrays.asList(cards)));
    }

    @Test
    public void tieLoses() {
        HiLoGame g = game(c(CardRank.FIVE), c(CardRank.FIVE));
        assertFalse(g.guess(true));
    }

    @Test
    public void twoCorrectPaysDouble() {
        HiLoGame g = game(c(CardRank.FIVE), c(CardRank.NINE), c(CardRank.TWO));
        assertTrue(g.guess(true));
        assertEquals(5, g.netIfStopped());
        assertTrue(g.guess(false));
        assertTrue(g.isFinished());
        assertEquals(10, g.netIfStopped());
    }

    @Test
    public void aceIsHigh() {
        HiLoGame g = game(c(CardRank.KING), c(CardRank.ACE));
        assertTrue(g.guess(true));
    }
}

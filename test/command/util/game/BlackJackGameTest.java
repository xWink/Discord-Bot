package command.util.game;

import command.util.cards.Card;
import command.util.cards.CardCodec;
import command.util.cards.CardRank;
import command.util.cards.CardSuit;
import command.util.cards.DeckOfCards;
import command.util.cards.HandOfCards;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class BlackJackGameTest {

    private static Card c(CardRank r) {
        return new Card(r, CardSuit.SPADES);
    }

    private static BlackJackGame game(int bet, Card[] player, Card[] dealer, Card... deck) {
        return new BlackJackGame(new Player(1, new HandOfCards(Arrays.asList(player))),
                new Player(0, new HandOfCards(Arrays.asList(dealer))), bet,
                DeckOfCards.of(Arrays.asList(deck)), "test");
    }

    @Test
    public void doubleBustIsALossNotATie() {
        BlackJackGame g = game(10, new Card[]{c(CardRank.TEN), c(CardRank.NINE), c(CardRank.FIVE)},
                new Card[]{c(CardRank.TEN), c(CardRank.SIX)}, c(CardRank.KING));
        assertEquals(-10, g.checkWinner());
        // The dealer never played.
        assertEquals(2, g.getDealer().getHand().getAsList().size());
    }

    @Test
    public void naturalPaysThreeToTwoRoundedDown() {
        BlackJackGame g = game(5, new Card[]{c(CardRank.ACE), c(CardRank.KING)},
                new Card[]{c(CardRank.TEN), c(CardRank.NINE)});
        assertEquals(7, g.checkWinner());
    }

    @Test
    public void bothNaturalsPush() {
        BlackJackGame g = game(5, new Card[]{c(CardRank.ACE), c(CardRank.KING)},
                new Card[]{c(CardRank.ACE), c(CardRank.QUEEN)});
        assertEquals(0, g.checkWinner());
    }

    @Test
    public void dealerDrawsToSeventeenAndBusts() {
        BlackJackGame g = game(4, new Card[]{c(CardRank.TEN), c(CardRank.EIGHT)},
                new Card[]{c(CardRank.TEN), c(CardRank.SIX)}, c(CardRank.KING));
        assertEquals(4, g.checkWinner());
    }

    @Test
    public void equalHandsPush() {
        BlackJackGame g = game(4, new Card[]{c(CardRank.TEN), c(CardRank.EIGHT)},
                new Card[]{c(CardRank.NINE), c(CardRank.NINE)});
        assertEquals(0, g.checkWinner());
    }

    @Test
    public void cardCodecRoundTrips() {
        String text = "10 h,A s,K d";
        assertEquals(text, CardCodec.encode(CardCodec.decode(text)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cardCodecRejectsGarbage() {
        CardCodec.decode("zz q");
    }
}

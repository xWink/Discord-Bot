package integration;

import command.util.cards.Card;
import command.util.cards.CardRank;
import command.util.cards.CardSuit;
import command.util.cards.DeckOfCards;
import command.util.cards.HandOfCards;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import command.util.game.BlackJackStore;
import command.util.game.Player;
import economy.EconomyService;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class BlackJackIntegrationTest extends DbTestBase {

    private static Card c(CardRank r) { return new Card(r, CardSuit.HEARTS); }

    private BlackJackGame open(long u, int bet, String res) throws Exception {
        BlackJackGame g = new BlackJackGame(new Player(u, new HandOfCards(Arrays.asList(c(CardRank.TEN), c(CardRank.EIGHT)))),
                new Player(0, new HandOfCards(Arrays.asList(c(CardRank.NINE), c(CardRank.NINE)))), bet,
                DeckOfCards.of(Arrays.asList(c(CardRank.TWO), c(CardRank.THREE))), res);
        g.setChannelId(42);
        BlackJackStore.open(g);
        BlackJackList.addGame(g);
        return g;
    }

    @Test
    public void openReservesStakeAndSettleWinsOnce() throws Exception {
        long u = newUser();
        BlackJackGame g = open(u, 4, "bj:" + u);
        assertEquals(1, EconomyService.getInstance().getSpendable(u));
        assertEquals(0, BlackJackStore.settle(g)); // 18 vs 18 pushes
        assertEquals(5, EconomyService.getInstance().getSpendable(u));
        assertEquals(0, BlackJackStore.settle(g)); // settling twice is a no-op
    }

    @Test
    public void secondHandForSameUserIsRejectedAndReservesNothing() throws Exception {
        long u = newUser();
        open(u, 3, "bj:a" + u);
        try {
            open(u, 2, "bj:b" + u);
            fail();
        } catch (java.sql.SQLException expected) { }
        assertEquals(2, EconomyService.getInstance().getSpendable(u)); // only the first stake held
    }

    @Test
    public void overStakeIsRejected() throws Exception {
        long u = newUser();
        try {
            open(u, 6, "bj:" + u);
            fail();
        } catch (EconomyService.InsufficientFundsRuntime expected) { }
    }

    @Test
    public void recoverRestoresOpenHandsAndRefundsBrokenOnes() throws Exception {
        long good = newUser(), bad = newUser();
        BlackJackGame g = open(good, 2, "bj:" + good);
        open(bad, 3, "bj:" + bad);
        BlackJackList.removeGame(g);
        exec("UPDATE blackjack_games SET deck = 'garbage' WHERE user = " + bad);
        List<BlackJackGame> restored = BlackJackStore.recover();
        assertTrue(restored.stream().anyMatch(x -> x.getPlayer().getUserId() == good));
        assertEquals(5, EconomyService.getInstance().getSpendable(bad)); // refunded
        assertNotNull(BlackJackList.getUserGame(good));
    }
}

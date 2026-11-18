package integration;

import command.Command;
import command.commands.economy.CoinDuel;
import command.commands.economy.Race;
import economy.EconomyService;
import net.dv8tion.jda.api.entities.User;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Games that settle between players: a coin duel and a trivia race. */
public class GamesIntegrationTest extends DbTestBase {

    private final EconomyService eco = EconomyService.getInstance();

    @Before
    public void noPause() {
        economy.Wagers.setCooldownMs(0);
    }

    private static String say(Command c, long user, String content, long channel, User... mentions) {
        FakeDiscord f = FakeDiscord.say(user, "P" + user % 1000, content, channel, mentions);
        assertTrue(content + " does not match", c.keyMatches(content));
        c.start(f.event);
        return f.said();
    }

    @Test
    public void coinDuelMovesTheStakeFromLoserToWinnerAndHoldsNothing() throws Exception {
        long a = newUser(), b = newUser();
        eco.credit(a, 5, "t", null, "ca" + a); // 10 each
        eco.credit(b, 5, "t", null, "cb" + b);
        long ch = main.Server.BOTS_CHANNEL_ID;
        String asked = say(new CoinDuel.Start(), a, "!coinduel <@" + b + "> 4", ch, FakeDiscord.user(b, "B"));
        assertTrue(asked, asked.contains("coin duel"));
        say(new CoinDuel.Reply("!coinaccept"), b, "!coinaccept", ch);
        int ba = eco.getBalance(a);
        int bb = eco.getBalance(b);
        assertEquals(20, ba + bb);
        assertTrue((ba == 14 && bb == 6) || (ba == 6 && bb == 14));
        assertEquals(0, eco.getReserved(a));
        assertEquals(0, eco.getReserved(b));
        // a second accept finds nothing waiting
        assertTrue(say(new CoinDuel.Reply("!coinaccept"), b, "!coinaccept", ch).contains("no coin duel"));
    }

    @Test
    public void coinDuelDeclineAndUnaffordableStakeChargeNothing() throws Exception {
        long a = newUser(), b = newUser();
        long ch = main.Server.BOTS_CHANNEL_ID;
        say(new CoinDuel.Start(), a, "!coinduel <@" + b + "> 3", ch, FakeDiscord.user(b, "B"));
        assertTrue(say(new CoinDuel.Reply("!coindecline"), b, "!coindecline", ch).contains("declined"));
        assertEquals(5, eco.getBalance(a));
        // 50 GC is more than anyone here has
        String refused = say(new CoinDuel.Start(), a, "!coinduel <@" + b + "> 50", ch, FakeDiscord.user(b, "B"));
        assertTrue(refused, refused.contains("spendable"));
    }

    @Test
    public void raceGivesTheHostsStakeToTheFirstCorrectAnswer() throws Exception {
        arcade.QuestionBank.loadBundled();
        long host = newUser(), winner = newUser(), loser = newUser();
        long ch = 5_000_000L + (long) (Math.random() * 1_000_000);
        say(new Race.Start(), host, "!race 3", ch);
        Race.Round round = Race.current(ch);
        assertNotNull(round);
        assertEquals(2, eco.getSpendable(host));
        int right = round.question.getCorrectIndex();
        int wrong = (right + 1) % round.question.getChoices().size();
        String l = String.valueOf((char) ('a' + wrong));
        assertTrue(say(new Race.Answer(), loser, "!rans " + l, ch).contains("is out"));
        assertTrue(say(new Race.Answer(), host, "!rans a", ch).contains("cannot answer"));
        say(new Race.Answer(), winner, "!rans " + (char) ('a' + right), ch);
        assertNull(Race.current(ch));
        assertEquals(8, eco.getBalance(winner));
        assertEquals(2, eco.getBalance(host));
        assertEquals(0, eco.getReserved(host));
        assertEquals(5, eco.getBalance(loser));
    }

    @Test
    public void raceThatExpiresGivesTheHostTheirStakeBack() throws Exception {
        arcade.QuestionBank.loadBundled();
        long host = newUser();
        long ch = 6_000_000L + (long) (Math.random() * 1_000_000);
        say(new Race.Start(), host, "!race 2", ch);
        assertNotNull(Race.current(ch));
        // the lifetime is real time, so wait it out
        Thread.sleep(Race.LIFETIME_MS + 500);
        assertTrue(Race.sweep().stream().anyMatch(r -> r.channelId == ch));
        assertEquals(5, eco.getSpendable(host));
        assertNull(Race.current(ch));
    }
}

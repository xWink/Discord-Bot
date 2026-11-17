package integration;

import arcade.PersonalTrivia;
import arcade.QuestionBank;
import economy.Achievements;
import economy.BannerService;
import economy.Cosmetics;
import economy.CosmeticShop;
import economy.EconomyService;
import economy.InsufficientFundsException;
import economy.SeasonService;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class FinishIntegrationTest extends DbTestBase {

    private final EconomyService eco = EconomyService.getInstance();

    @Test
    public void achievementsUnlockOnceAndCountOnCollectionBoard() throws Exception {
        long u = newUser();
        assertTrue(Achievements.award(u, "duel-win"));
        assertFalse(Achievements.award(u, "duel-win"));
        assertEquals(1, Achievements.unlocked(u).size());
        List<SeasonService.Row> board = SeasonService.live("collection", SeasonService.currentSeason());
        assertTrue(board.stream().anyMatch(r -> r.user == u && r.score == 1));
    }

    @Test
    public void realActionsAwardAchievements() throws Exception {
        QuestionBank.loadBundled();
        long u = newUser();
        arcade.ArcadeService.Round r = arcade.ArcadeService.start(u);
        arcade.ArcadeService.answer(u, r.question.getCorrectIndex());
        assertTrue(Achievements.unlocked(u).contains("first-correct"));
    }

    @Test
    public void equipNeedsOwnershipAndShowsOnlyUnexpiredItems() throws Exception {
        long u = newUser();
        eco.credit(u, 100, "t", null, "c" + u);
        assertNull(Cosmetics.equip(u, "title-week")); // not owned
        CosmeticShop.getInstance().buy(u, "title-week", "i" + u);
        CosmeticShop.getInstance().buy(u, "flair-day", "j" + u);
        assertEquals("title", Cosmetics.equip(u, "title-week"));
        assertEquals("flair", Cosmetics.equip(u, "flair-day"));
        Cosmetics.Extras ex = Cosmetics.extras(u);
        assertEquals(" the Scholar", ex.title);
        assertNotNull(ex.color);
        assertTrue(ex.badges.toString().contains("Daily flair"));
        assertEquals("Ann the Scholar", Cosmetics.nameWithTitle(u, "Ann"));

        exec("UPDATE inventory SET expires_at = 1 WHERE user = " + u + " AND item_code = 'title-week'");
        assertEquals("", Cosmetics.extras(u).title); // expired items stop rendering
        Cosmetics.unequipAll(u);
        assertEquals("", Cosmetics.extras(u).badges.toString());
    }

    @Test
    public void bannerNeedsApprovalAndRefundsOtherwise() throws Exception {
        BannerService b = BannerService.getInstance();
        long u = newUser();
        eco.credit(u, 95, "t", null, "c" + u);
        long id = b.request(u, 77L, "Gryphon Hockey");
        assertTrue(id > 0);
        assertEquals(55, eco.getSpendable(u));
        assertEquals(-1, b.request(u, 77L, "Again"));
        assertTrue(b.approve(id));
        assertFalse(b.approve(id));
        assertEquals(55, eco.getBalance(u));
        assertNotNull(Cosmetics.equip(u, "banner-club"));
        assertTrue(Cosmetics.extras(u).badges.toString().contains("Gryphon Hockey banner"));

        long id2 = b.request(u, 77L, "Declined");
        assertTrue(b.decline(id2));
        assertEquals(55, eco.getSpendable(u));

        long id3 = b.request(u, 77L, "Slow team");
        exec("UPDATE banner_requests SET created_at = 1 WHERE id = " + id3);
        assertTrue(b.expireUnanswered() >= 1);
        assertEquals(55, eco.getSpendable(u));
        try {
            b.request(newUser(), 77L, "poor");
            fail();
        } catch (InsufficientFundsException expected) { }
        // The banner cannot be bought around the approval step.
        try {
            assertNull(CosmeticShop.getInstance().buy(u, "banner-club", "direct" + u));
        } catch (InsufficientFundsException ignored) { }
    }

    @Test
    public void personalTriviaChargesHostOnlyAndCountsForJoiners() throws Exception {
        QuestionBank.loadBundled();
        long host = newUser(), friend = newUser();
        long channel = 5_000_000L + (long) (Math.random() * 1_000_000);
        eco.credit(host, 10, "t", null, "c" + host); // 15
        Object r = PersonalTrivia.start(host, channel, "geo", 7, "pt" + host);
        assertTrue(r instanceof PersonalTrivia.Round);
        assertEquals(8, eco.getBalance(host));
        assertTrue(PersonalTrivia.start(host, channel, "geo", 7, "pt2" + host) instanceof String); // one at a time

        int right = ((PersonalTrivia.Round) r).question.getCorrectIndex();
        arcade.ArcadeService.Result res = PersonalTrivia.answer(channel, friend, right);
        assertTrue(res.correct && res.dailyPaid);
        assertEquals(10, eco.getBalance(friend)); // 5 start + 5 daily, no cost to join
        assertNull(PersonalTrivia.answer(channel, friend, right)); // one answer each

        assertTrue(PersonalTrivia.start(newUser(), channel + 1, "nonexistent-category", 5, "x") instanceof String);
        assertTrue(PersonalTrivia.start(host, channel + 2, "geo", 11, "y") instanceof String); // cost range
    }
}

package integration;

import arcade.ArcadeService;
import arcade.PunchCard;
import arcade.Question;
import arcade.QuestionBank;
import database.connectors.BangResults;
import economy.EconomyService;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ArcadeIntegrationTest extends DbTestBase {

    @Test
    public void bundledQuestionsLoadOnceAndServe() throws Exception {
        QuestionBank.loadBundled();
        int first = QuestionBank.stock("code", null);
        QuestionBank.loadBundled();
        assertEquals(first, QuestionBank.stock("code", null));
        assertTrue(first >= 10);
        Question q = QuestionBank.next("trivia", null, newUser());
        assertNotNull(q);
        assertTrue(q.getChoices().size() >= 2);
    }

    @Test
    public void duplicateQuestionsAreIgnored() throws Exception {
        assertTrue(QuestionBank.insert("test", "trivia", "T", "easy", "Unique " + System.nanoTime() + "?",
                Arrays.asList("a", "b"), 0, null, "x", "y"));
        assertFalse(QuestionBank.insert("test", "trivia", "T", "easy", "Dup?", Arrays.asList("a", "b"), 0, null,
                "x", "y") && QuestionBank.insert("test", "trivia", "T", "easy", "dup?", Arrays.asList("B", "A"), 1,
                null, "x", "y"));
    }

    @Test
    public void roundPaysDailyOnceAndPunchCardOnThirdDay() throws Exception {
        QuestionBank.loadBundled();
        long u = newUser();
        ArcadeService.Round r = ArcadeService.start(u);
        assertNotNull(r);
        ArcadeService.Result res = ArcadeService.answer(u, r.question.getCorrectIndex());
        assertTrue(res.correct);
        assertTrue(res.dailyPaid);
        assertEquals(10, EconomyService.getInstance().getBalance(u));

        r = ArcadeService.start(u);
        res = ArcadeService.answer(u, 0);
        assertFalse(res.dailyPaid); // replay is free practice
        assertNull(ArcadeService.answer(u, 0)); // round consumed

        // Two more distinct days in the same week trigger the punch card exactly once.
        int week = PunchCard.currentWeek();
        exec("INSERT IGNORE INTO arcade_days (user, day, week) VALUES (" + u + ", 1000001, " + week + "), ("
                + u + ", 1000002, " + week + ")");
        assertTrue(PunchCard.record(u));
        assertFalse(PunchCard.record(u));
        assertEquals(20, EconomyService.getInstance().getBalance(u));
    }

    @Test
    public void weeklyBoardQueryRuns() throws Exception {
        assertNotNull(ArcadeService.weeklyBoard());
    }

    @Test
    public void bangResultsInsert() throws Exception {
        BangResults.record(newUser(), System.currentTimeMillis(), true, false);
    }
}

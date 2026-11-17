package integration;

import arcade.PunchCard;
import arcade.Question;
import arcade.QuestionBank;
import arcade.QuizService;
import economy.BountyService;
import economy.EconomyService;
import economy.EventService;
import economy.InsufficientFundsException;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class SocialIntegrationTest extends DbTestBase {

    private final EconomyService eco = EconomyService.getInstance();

    @Test
    public void bountyLifecyclePaysWorkerAndRefundsCancelled() throws Exception {
        BountyService b = BountyService.getInstance();
        long poster = newUser(), worker = newUser();
        eco.credit(poster, 45, "t", null, "c" + poster); // 50
        long id = b.post(poster, 20, "design an emoji");
        assertEquals(30, eco.getSpendable(poster));
        assertFalse(b.claim(id, poster)); // cannot claim your own
        assertTrue(b.claim(id, worker));
        assertEquals(worker, b.accept(id, poster));
        assertEquals(30, eco.getBalance(poster));
        assertEquals(25, eco.getBalance(worker));
        assertEquals(0, b.accept(id, poster)); // pays once

        long id2 = b.post(poster, 10, "test a feature");
        assertTrue(b.cancel(id2, poster));
        assertEquals(30, eco.getSpendable(poster));
    }

    @Test
    public void bountyLimitsAndExpiry() throws Exception {
        BountyService b = BountyService.getInstance();
        long poster = newUser();
        eco.credit(poster, 95, "t", null, "c" + poster);
        for (int i = 0; i < BountyService.MAX_OPEN_PER_POSTER; i++) assertTrue(b.post(poster, 5, "x") > 0);
        assertEquals(-1, b.post(poster, 5, "x"));
        try {
            b.post(newUser(), 50, "too much");
            fail();
        } catch (InsufficientFundsException expected) { }
        exec("UPDATE bounties SET expires_at = 1 WHERE poster = " + poster);
        assertTrue(b.expireOverdue() >= 3);
        assertEquals(100, eco.getSpendable(poster));
    }

    @Test
    public void eventPaysWithinWeeklyCapAndNeedsPassForPlayers() throws Exception {
        EventService e = EventService.getInstance();
        long host = newUser(), p = newUser();
        assertEquals(-2, e.start(host, "trivia night", false));
        exec("INSERT INTO inventory (user, item_code, purchase_id, expires_at, created_at) VALUES (" + host
                + ", 'event-pass', 'pass-" + host + "', 0, 1)");
        long first = e.start(host, "trivia night", false);
        assertTrue(first > 0);
        assertEquals(-1, e.start(host, "again", true));
        assertEquals(1, e.confirm(host, Arrays.asList(p, host)));
        assertEquals(0, e.confirm(host, Arrays.asList(p)));
        EventService.Result r = e.end(host);
        assertEquals(5, r.totalPaid);
        // Event attendance also completed the weekly challenge (10 GC) - 5 + 10 on a fresh 5 GC wallet.
        assertEquals(20, eco.getBalance(p));
        assertNull(e.end(host));

        for (int i = 0; i < 2; i++) {
            e.start(host, "e" + i, true);
            e.confirm(host, Arrays.asList(p));
            e.end(host);
        }
        assertEquals(25, eco.getBalance(p)); // weekly cap of 10 GC reached after the second event
    }

    @Test
    public void weeklyRunPaysOncePerWeekAndGauntletPrizeSpendsEntry() throws Exception {
        QuestionBank.loadBundled();
        long u = newUser();
        Object r = QuizService.start(u, QuizService.Mode.WEEKLY, "code", 1, "m" + u);
        QuizService.Session s = (QuizService.Session) r;
        QuizService.Step st = null;
        for (int i = 0; i < QuizService.QUESTIONS; i++) {
            st = QuizService.answer(u, QuizService.current(u).current().getCorrectIndex());
        }
        assertEquals(15, st.reward);
        assertEquals(20, eco.getBalance(u));
        assertTrue(QuizService.start(u, QuizService.Mode.WEEKLY, "code", 1, "n" + u) instanceof String);

        Object g = QuizService.start(u, QuizService.Mode.PRIZE, null, 1, "g" + u);
        assertTrue(g instanceof QuizService.Session);
        assertEquals(17, eco.getSpendable(u));
        for (int i = 0; i < QuizService.QUESTIONS; i++) {
            st = QuizService.answer(u, QuizService.current(u) == null ? 0
                    : QuizService.current(u).current().getCorrectIndex());
        }
        assertTrue(st.prizeAwarded);
        assertEquals(17, eco.getBalance(u));
        assertEquals(0, eco.getReserved(u));
    }

    @Test
    public void abandonedPrizeRunRefundsOnRestartRelease() throws Exception {
        QuestionBank.loadBundled();
        long u = newUser();
        eco.credit(u, 0 + 1, "t", null, "c" + u);
        assertTrue(QuizService.start(u, QuizService.Mode.PRIZE, null, 1, "x" + u) instanceof QuizService.Session);
        assertEquals(3, eco.getSpendable(u));
        assertTrue(eco.refundAllByReason("gauntlet") >= 1);
        assertEquals(6, eco.getSpendable(u));
    }
}

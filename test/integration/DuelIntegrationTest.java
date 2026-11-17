package integration;

import arcade.Question;
import duel.Duel;
import duel.DuelService;
import economy.EconomyService;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.*;

public class DuelIntegrationTest extends DbTestBase {

    private Duel started(long a, long b, int stake) throws Exception {
        Duel d = DuelService.challenge(a, b, stake, 1);
        assertNotNull(d);
        assertNull(DuelService.accept(d)); // needs questions in the bank
        return d;
    }

    @Test
    public void winnerGetsPotMinusFeeLoserLosesStake() throws Exception {
        arcade.QuestionBank.loadBundled();
        long a = newUser(), b = newUser();
        EconomyService.getInstance().credit(a, 5, "t", null, "ca" + a); // 10
        EconomyService.getInstance().credit(b, 5, "t", null, "cb" + b);
        Duel d = started(a, b, 5);
        assertEquals(5, EconomyService.getInstance().getSpendable(a));
        int right = d.getQuestion().getCorrectIndex();
        int wrong = (right + 1) % d.getQuestion().getChoices().size();
        assertNull(DuelService.answer(a, right));
        DuelService.Outcome o = DuelService.answer(b, wrong);
        assertEquals(a, o.winner);
        assertEquals(14, EconomyService.getInstance().getBalance(a)); // 10 + 5 - 1 fee
        assertEquals(5, EconomyService.getInstance().getBalance(b));
        assertNotNull(o.ratings);
        assertNull(DuelService.activeFor(a));
    }

    @Test
    public void drawRefundsBothStakes() throws Exception {
        arcade.QuestionBank.loadBundled();
        long a = newUser(), b = newUser();
        Duel d = started(a, b, 3);
        int wrong = (d.getQuestion().getCorrectIndex() + 1) % d.getQuestion().getChoices().size();
        DuelService.answer(a, wrong);
        DuelService.Outcome o = DuelService.answer(b, wrong);
        assertEquals(0, o.winner);
        assertEquals(5, EconomyService.getInstance().getBalance(a));
        assertEquals(0, EconomyService.getInstance().getReserved(a));
    }

    @Test
    public void unaffordableStakeCancelsWithoutCharging() throws Exception {
        arcade.QuestionBank.loadBundled();
        long a = newUser(), b = newUser();
        Duel d = DuelService.challenge(a, b, 9, 1);
        assertNotNull(DuelService.accept(d));
        assertEquals(5, EconomyService.getInstance().getSpendable(a));
        assertNull(DuelService.activeFor(a));
    }

    @Test
    public void busyPlayersCannotBeChallenged() throws Exception {
        long a = newUser(), b = newUser(), c = newUser();
        assertNotNull(DuelService.challenge(a, b, 0, 1));
        assertNull(DuelService.challenge(c, b, 0, 1));
        assertNull(DuelService.challenge(a, c, 0, 1));
    }
}

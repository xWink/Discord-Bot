package command.util.game;

import command.util.game.SlotsGame.Symbol;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SlotsGameTest {

    @Test
    public void weightsAddUpToOneHundred() {
        int total = 0;
        for (Symbol s : Symbol.values()) total += s.weight();
        assertEquals(100, total);
    }

    @Test
    public void payoutsFollowThePayTable() {
        assertEquals(15 * 5, SlotsGame.payout(new Symbol[]{Symbol.STAR, Symbol.STAR, Symbol.STAR}, 5));
        assertEquals(50 * 2, SlotsGame.payout(new Symbol[]{Symbol.MASCOT, Symbol.MASCOT, Symbol.MASCOT}, 2));
        assertEquals(7, SlotsGame.payout(new Symbol[]{Symbol.BELL, Symbol.LEMON, Symbol.BELL}, 5)); // 1.4x, rounded down
        assertEquals(0, SlotsGame.payout(new Symbol[]{Symbol.BELL, Symbol.LEMON, Symbol.SEVEN}, 5));
        assertEquals(-5, SlotsGame.net(new Symbol[]{Symbol.BELL, Symbol.LEMON, Symbol.SEVEN}, 5));
        assertEquals(0, SlotsGame.net(new Symbol[]{Symbol.CHERRY, Symbol.CHERRY, Symbol.BELL}, 1)); // a push
    }

    @Test
    public void returnToPlayerIsAboutNinetyFivePercent() {
        // exact expectation over every reel combination, weighted
        double expected = 0;
        Symbol[] all = Symbol.values();
        for (Symbol a : all) {
            for (Symbol b : all) {
                for (Symbol c : all) {
                    double p = a.weight() / 100.0 * b.weight() / 100.0 * c.weight() / 100.0;
                    expected += p * SlotsGame.payout(new Symbol[]{a, b, c}, 1000) / 1000.0;
                }
            }
        }
        assertTrue("return was " + expected, expected > 0.93 && expected < 0.97);
    }

    @Test
    public void labelsDescribeTheResult() {
        assertEquals("THREE STARS x15", SlotsGame.label(new Symbol[]{Symbol.STAR, Symbol.STAR, Symbol.STAR}));
        assertEquals("PAIR x1.4", SlotsGame.label(new Symbol[]{Symbol.STAR, Symbol.BELL, Symbol.STAR}));
        assertEquals("NO MATCH", SlotsGame.label(new Symbol[]{Symbol.STAR, Symbol.BELL, Symbol.CHERRY}));
    }
}

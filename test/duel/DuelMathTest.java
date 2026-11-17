package duel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DuelMathTest {

    @Test
    public void fiveGcStakeLeavesNineInThePot() {
        assertEquals(1, DuelMath.fee(5));
        assertEquals(4, DuelMath.winnerNet(5)); // 5 own + 5 other - 1 fee = 9 payout
        assertEquals(0, DuelMath.fee(4));
        assertEquals(4, DuelMath.winnerNet(4));
    }

    @Test
    public void equalRatingsMoveSixteen() {
        int[] r = DuelMath.update(1000, 1000, true);
        assertEquals(1016, r[0]);
        assertEquals(984, r[1]);
    }

    @Test
    public void upsetMovesMoreThanExpectedWin() {
        int gainUnderdog = DuelMath.update(900, 1100, true)[0] - 900;
        int gainFavourite = DuelMath.update(1100, 900, true)[0] - 1100;
        assertTrue(gainUnderdog > gainFavourite);
    }
}

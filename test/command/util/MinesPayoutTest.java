package command.util;

import command.util.game.MinesGame;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

public class MinesPayoutTest {

    @Test
    public void maxStakeKeepsEveryPayoutInRange() {
        for (int mines = MinesGame.MIN_MINES; mines <= MinesGame.MAX_MINES; mines++) {
            double top = MinesGame.maxStake(mines) * MinesGame.multiplier(mines, MinesGame.TILES - mines);
            assertTrue("mines=" + mines, top <= Integer.MAX_VALUE);
            assertTrue(new MinesGame(1, MinesGame.maxStake(mines), "r", mines, new Random(1)).netIfStopped() >= 0);
        }
    }
}

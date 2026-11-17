package command.util.game;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiceGameTest {

    @Test
    public void sevenIsNeitherLowNorHigh() {
        assertFalse(DiceGame.wins(DiceGame.Pick.LOW, 7));
        assertFalse(DiceGame.wins(DiceGame.Pick.HIGH, 7));
        assertTrue(DiceGame.wins(DiceGame.Pick.SEVEN, 7));
    }

    @Test
    public void payoutsFollowPublishedTable() {
        assertEquals(5, DiceGame.net(DiceGame.Pick.HIGH, 9, 5));
        assertEquals(-5, DiceGame.net(DiceGame.Pick.HIGH, 5, 5));
        assertEquals(20, DiceGame.net(DiceGame.Pick.SEVEN, 7, 5));
        assertEquals(-5, DiceGame.net(DiceGame.Pick.SEVEN, 8, 5));
    }

    @Test
    public void publishedWinChancesAreCorrect() {
        int low = 0, high = 0, seven = 0;
        for (int a = 1; a <= 6; a++) {
            for (int b = 1; b <= 6; b++) {
                if (DiceGame.wins(DiceGame.Pick.LOW, a + b)) low++;
                if (DiceGame.wins(DiceGame.Pick.HIGH, a + b)) high++;
                if (DiceGame.wins(DiceGame.Pick.SEVEN, a + b)) seven++;
            }
        }
        assertEquals(15, low);
        assertEquals(15, high);
        assertEquals(6, seven);
    }
}

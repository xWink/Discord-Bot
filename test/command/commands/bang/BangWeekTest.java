package command.commands.bang;

import database.connectors.BangResults;
import integration.DbTestBase;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BangWeekTest extends DbTestBase {

    @Test
    public void bestStreakCountsConsecutiveSurvivalsAndNeedsMinimumAttempts() throws Exception {
        long user = newUser(), few = newUser();
        long t = System.currentTimeMillis();
        // pattern: 6 survive, die, 9 survive, die, 3 survive = 20 attempts, best run 9
        int[] died = {0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0};
        for (int i = 0; i < died.length; i++) BangResults.record(user, t + i, died[i] == 1, false);
        for (int i = 0; i < 5; i++) BangResults.record(few, t + i, false, false);
        Map<Long, Integer> best = BangWeek.bestStreaks(t - 1);
        assertEquals(9, best.get(user).intValue());
        assertFalse(best.containsKey(few));
    }
}

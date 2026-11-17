package economy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DecayMathTest {

    private static int[] simulate(int balance, int weeks) {
        long carry = 0;
        for (int i = 0; i < weeks; i++) {
            DecayMath.Step s = DecayMath.step(balance, carry, 0);
            balance -= s.charged;
            carry = s.carryMilli;
        }
        return new int[]{balance};
    }

    @Test
    public void protectedAmountNeverDecays() {
        assertEquals(100, simulate(100, 12)[0]);
        assertEquals(40, simulate(40, 12)[0]);
    }

    @Test
    public void matchesPublishedTable() {
        assertEquals(930, simulate(1000, 4)[0], 1);
        assertEquals(806, simulate(1000, 12)[0], 2);
        assertEquals(2314, simulate(2500, 4)[0], 2);
        assertEquals(1983, simulate(2500, 12)[0], 3);
    }

    @Test
    public void fractionsAreCarriedNotLost() {
        // 110 GC: excess 10 -> 0.2 GC a week. Five weeks add up to one whole coin.
        long carry = 0;
        int charged = 0;
        int balance = 110;
        for (int i = 0; i < 5; i++) {
            DecayMath.Step s = DecayMath.step(balance, carry, 0);
            charged += s.charged;
            balance -= s.charged;
            carry = s.carryMilli;
        }
        assertEquals(1, charged);
    }

    @Test
    public void reservedCoinsAreAssessedButNotCollected() {
        // 1000 GC with 990 reserved: 18 GC owed, only 10 unreserved to take.
        DecayMath.Step s = DecayMath.step(1000, 0, 990);
        assertEquals(10, s.charged);
        assertEquals(8, s.shortfall);
    }
}

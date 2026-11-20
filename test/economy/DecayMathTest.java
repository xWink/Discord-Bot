package economy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DecayMathTest {

    private static int simulate(int balance, int weeks) {
        for (int i = 0; i < weeks; i++) {
            balance -= DecayMath.step(balance, 0).charged;
        }
        return balance;
    }

    @Test
    public void protectedAmountNeverDecays() {
        assertEquals(100, simulate(100, 12));
        assertEquals(40, simulate(40, 12));
    }

    @Test
    public void twoPercentOfTheExcessRoundedDown() {
        assertEquals(18, DecayMath.owed(1000)); // 2% of 900
        assertEquals(17, DecayMath.owed(982));  // 17.64 -> 17
        assertEquals(1, DecayMath.owed(150));   // exactly 1
        assertEquals(DecayMath.owed(1000), DecayMath.estimateNext(1000));
    }

    @Test
    public void smallExcessPaysNothing() {
        // 49 GC above the line is 0.98 GC: rounds down to nothing, and nothing is carried to later weeks.
        assertEquals(0, DecayMath.owed(149));
        assertEquals(149, simulate(149, 52));
        assertEquals(110, simulate(110, 52));
    }

    @Test
    public void matchesWholeCoinTable() {
        assertEquals(932, simulate(1000, 4));
        assertEquals(811, simulate(1000, 12));
        assertEquals(2314, simulate(2500, 4));
        assertEquals(1987, simulate(2500, 12));
    }

    @Test
    public void chargesAreAlwaysWholeAndNeverGoBelowTheProtectedAmount() {
        for (int balance = 0; balance <= 5000; balance += 7) {
            int owed = DecayMath.owed(balance);
            assertEquals(owed, (long) owed);
            if (balance >= DecayMath.PROTECTED) assertEquals(true, balance - owed >= DecayMath.PROTECTED);
            else assertEquals(0, owed);
        }
    }

    @Test
    public void reservedCoinsAreAssessedButNotCollected() {
        // 1000 GC with 990 reserved: 18 GC owed, only 10 unreserved to take.
        DecayMath.Step s = DecayMath.step(1000, 990);
        assertEquals(10, s.charged);
        assertEquals(8, s.shortfall);
    }
}

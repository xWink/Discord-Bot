package economy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PriceMathTest {

    @Test
    public void smallSampleHoldsPrice() {
        assertEquals(20, PriceMath.next(20, 20, 4, 4));
    }

    @Test
    public void strongDemandRaisesByTenPercentOfBase() {
        assertEquals(22, PriceMath.next(20, 20, 6, 10));
    }

    @Test
    public void weakDemandDropsAndStaysInsideRange() {
        assertEquals(18, PriceMath.next(20, 20, 1, 10));
        assertEquals(16, PriceMath.next(20, 16, 0, 10)); // floor is 80% of base
        assertEquals(24, PriceMath.next(20, 24, 9, 12)); // ceiling is 120% of base
    }

    @Test
    public void normalDemandHolds() {
        assertEquals(20, PriceMath.next(20, 20, 3, 10));
    }
}

package integration;

import economy.CosmeticShop;
import economy.EconomyService;
import economy.PriceChangedException;
import economy.PricingService;
import org.junit.Test;

import static org.junit.Assert.*;

public class PricingIntegrationTest extends DbTestBase {

    @Test
    public void rotationRunsOncePerWeekAndFeaturesThreeItems() throws Exception {
        exec("DELETE FROM economy_settings WHERE name = 'shop_rotation_week'");
        assertTrue(PricingService.rotateIfDue());
        assertFalse(PricingService.rotateIfDue());
        int featured = 0;
        for (CosmeticShop.Item i : CosmeticShop.getInstance().catalog()) if (i.featured) featured++;
        assertEquals(PricingService.FEATURED_COUNT, featured);
    }

    @Test
    public void staleQuoteIsRefusedWithoutCharging() throws Exception {
        long u = newUser();
        EconomyService.getInstance().credit(u, 95, "t", null, "c" + u);
        exec("UPDATE shop_items SET price = 20, base_price = 20 WHERE code = 'bundle-weekend'");
        try {
            CosmeticShop.getInstance().buy(u, "bundle-weekend", "q1-" + u, 18); // quoted 18, now 20
            fail();
        } catch (PriceChangedException e) {
            assertEquals(20, e.getNewPrice());
        }
        assertEquals(100, EconomyService.getInstance().getBalance(u));
        assertNotNull(CosmeticShop.getInstance().buy(u, "bundle-weekend", "q2-" + u, 20));
        assertEquals(80, EconomyService.getInstance().getBalance(u));
    }

    @Test
    public void installmentsAreFinalCappedAndDeliverOnce() throws Exception {
        economy.InstallmentService svc = economy.InstallmentService.getInstance();
        long u = newUser();
        EconomyService.getInstance().credit(u, 2995, "t", null, "c" + u); // 3000
        assertNotNull(svc.start(u, 555L, "orange"));
        assertNull(svc.start(u, 555L, "blue")); // one plan at a time
        assertEquals(1000, svc.pay(u, 1000, "p1" + u).paid);
        assertEquals(2500, svc.pay(u, 9000, "p2" + u).paid); // capped at what is left
        assertEquals("GRANTING", svc.current(u).status);
        assertEquals(500, EconomyService.getInstance().getBalance(u));
        assertTrue(svc.markGranted(svc.current(u).id));
        assertNull(svc.current(u));
    }
}

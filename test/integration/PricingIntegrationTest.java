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

    @Test
    public void colourSwapKeepsRemainingTimeAndRefundsOnFailure() throws Exception {
        economy.ShopService shop = economy.ShopService.getInstance();
        long u = newUser();
        EconomyService.getInstance().credit(u, 95, "t", null, "c" + u);
        assertEquals(-1, shop.beginSwap("s0" + u, u, "blue", 2L)); // no rental yet
        long expiry = System.currentTimeMillis() + 3 * 86400000L;
        exec("UPDATE economy SET role_expiry = " + expiry + ", role_colour = 'orange', role_id = 1 WHERE user = " + u);
        assertEquals(1, shop.beginSwap("s1" + u, u, "blue", 2L));
        assertEquals(95, EconomyService.getInstance().getBalance(u));
        assertTrue(shop.complete("s1" + u, "blue"));
        assertEquals(2L, new database.connectors.EconomyConnector().getRoleId(u));
        assertEquals(expiry, new java.util.concurrent.atomic.AtomicLong(
                database.Database.inTransaction(c -> {
                    try (java.sql.ResultSet rs = c.createStatement().executeQuery(
                            "SELECT role_expiry FROM economy WHERE user = " + u)) {
                        rs.next();
                        return rs.getLong(1);
                    }
                })).get());
        shop.beginSwap("s2" + u, u, "pink", 3L);
        assertTrue(shop.failAndRefund("s2" + u));
        assertEquals(95, EconomyService.getInstance().getBalance(u));
    }

    @Test
    public void wrappedGiftChargesFeeSeparately() throws Exception {
        long a = newUser(), b = newUser();
        EconomyService.getInstance().credit(a, 20, "t", null, "c" + a); // 25
        EconomyService.getInstance().transferWithFee(a, b, 10, 3, "gift", null, "w" + a);
        assertEquals(12, EconomyService.getInstance().getBalance(a));
        assertEquals(15, EconomyService.getInstance().getBalance(b));
        try {
            EconomyService.getInstance().transferWithFee(a, b, 10, 3, "gift", null, "w2" + a); // 13 needed, 12 held
            fail();
        } catch (economy.InsufficientFundsException expected) { }
        assertEquals(12, EconomyService.getInstance().getBalance(a));
    }
}

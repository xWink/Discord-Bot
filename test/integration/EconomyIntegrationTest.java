package integration;

import economy.DailyService;
import economy.DecayService;
import economy.EconomyService;
import economy.GoalService;
import economy.InsufficientFundsException;
import economy.ShopService;
import economy.WagerLimits;
import economy.CosmeticShop;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class EconomyIntegrationTest extends DbTestBase {

    private final EconomyService eco = EconomyService.getInstance();

    @Test
    public void newWalletStartsWithFive() throws Exception {
        assertEquals(5, eco.getBalance(newUser()));
    }

    @Test
    public void creditIsIdempotentOnOpId() throws Exception {
        long u = newUser();
        eco.credit(u, 10, "t", null, "op-" + u);
        eco.credit(u, 10, "t", null, "op-" + u);
        assertEquals(15, eco.getBalance(u));
    }

    @Test
    public void transferMovesCoinsAndConserves() throws Exception {
        long a = newUser(), b = newUser();
        eco.credit(a, 20, "t", null, "c-" + a); // 25
        eco.transfer(a, b, 10, "gift", null, "g-" + a);
        eco.transfer(a, b, 10, "gift", null, "g-" + a); // retry: no double payment
        assertEquals(15, eco.getBalance(a));
        assertEquals(15, eco.getBalance(b));
    }

    @Test
    public void overdraftIsRejectedAndChangesNothing() throws Exception {
        long a = newUser(), b = newUser();
        try {
            eco.transfer(a, b, 6, "gift", null, "g-" + a);
            fail();
        } catch (InsufficientFundsException expected) {
            assertEquals(5, expected.getSpendable());
        }
        assertEquals(5, eco.getBalance(a));
        assertEquals(5, eco.getBalance(b));
    }

    @Test
    public void simultaneousSpendsCannotOverdraw() throws Exception {
        long u = newUser();
        eco.credit(u, 95, "t", null, "c-" + u); // 100
        ExecutorService pool = Executors.newFixedThreadPool(8);
        AtomicInteger ok = new AtomicInteger();
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            final int n = i;
            fs.add(pool.submit((Callable<Void>) () -> {
                try {
                    eco.debit(u, 30, "spend", null, "s-" + u + "-" + n);
                    ok.incrementAndGet();
                } catch (InsufficientFundsException ignored) { }
                return null;
            }));
        }
        for (Future<?> f : fs) f.get();
        pool.shutdown();
        assertEquals(3, ok.get());
        assertEquals(10, eco.getBalance(u));
    }

    @Test
    public void reservationBlocksSpendingAndSettlesOnce() throws Exception {
        long u = newUser();
        eco.credit(u, 5, "t", null, "c-" + u); // 10
        eco.reserve(u, 8, "blackjack", null, "r-" + u);
        assertEquals(2, eco.getSpendable(u));
        try {
            eco.debit(u, 5, "spend", null, "s-" + u);
            fail();
        } catch (InsufficientFundsException expected) { }
        assertTrue(eco.settleReservation("r-" + u, 12, "blackjack"));
        assertFalse(eco.settleReservation("r-" + u, 12, "blackjack"));
        assertEquals(22, eco.getBalance(u));
        assertEquals(0, eco.getReserved(u));
    }

    @Test
    public void lossSettlementDebitsStake() throws Exception {
        long u = newUser();
        eco.reserve(u, 5, "blackjack", null, "r-" + u);
        eco.settleReservation("r-" + u, -5, "blackjack");
        assertEquals(0, eco.getBalance(u));
    }

    @Test
    public void refundReleasesWithoutChange() throws Exception {
        long u = newUser();
        eco.reserve(u, 5, "hilo", null, "r-" + u);
        assertTrue(eco.refundReservation("r-" + u));
        assertEquals(5, eco.getSpendable(u));
    }

    @Test
    public void dailyPaysOnceEvenUnderRace() throws Exception {
        long u = newUser();
        ExecutorService pool = Executors.newFixedThreadPool(6);
        AtomicInteger paid = new AtomicInteger();
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            fs.add(pool.submit((Callable<Void>) () -> {
                if (DailyService.getInstance().claim(u, "test", System.currentTimeMillis())) paid.incrementAndGet();
                return null;
            }));
        }
        for (Future<?> f : fs) f.get();
        pool.shutdown();
        assertEquals(1, paid.get());
        assertEquals(10, eco.getBalance(u));
        assertTrue(DailyService.getInstance().nextEligibleAt(u) > System.currentTimeMillis());
    }

    @Test
    public void shopRefundsFailedGrantAndCompletesGoodOne() throws Exception {
        long u = newUser();
        eco.credit(u, 45, "t", null, "c-" + u); // 50
        ShopService shop = ShopService.getInstance();
        shop.begin("p1-" + u, u, "orange", 35, 123L, 7);
        assertEquals(15, eco.getBalance(u));
        assertTrue(shop.failAndRefund("p1-" + u));
        assertFalse(shop.failAndRefund("p1-" + u));
        assertEquals(50, eco.getBalance(u));

        shop.begin("p2-" + u, u, "orange", 35, 123L, 7);
        assertTrue(shop.complete("p2-" + u, "orange"));
        assertFalse(shop.complete("p2-" + u, "orange"));
        assertFalse(shop.failAndRefund("p2-" + u));
        assertEquals(15, eco.getBalance(u));
    }

    @Test
    public void weeklyDecayChargesAboveProtectedAndIsRepeatSafe() throws Exception {
        long u = newUser();
        eco.credit(u, 995, "t", null, "c-" + u); // 1000
        int week = DecayService.weekIndex(System.currentTimeMillis());
        // Pretend decay started 4 weeks ago, with this wallet last settled 4 weeks ago.
        DecayService.setFirstWeek(week - 3);
        exec("UPDATE economy SET last_decay_week = " + (week - 4) + " WHERE user = " + u);
        int after = eco.getBalance(u);
        assertEquals(932, after); // 18, 17, 17, 16 GC: 2% of the excess, rounded down each week
        assertEquals(after, eco.getBalance(u)); // second read changes nothing
        // Leave decay off for other tests.
        exec("DELETE FROM economy_settings WHERE name = 'decay_first_week'");
        DecayService.setFirstWeek(week + 1000);
    }

    @Test
    public void goalContributionIsCappedAtRemainingAndCancelRefunds() throws Exception {
        exec("UPDATE community_goals SET status = 'CANCELLED' WHERE status IN ('ACTIVE', 'REACHED')");
        long a = newUser(), b = newUser();
        eco.credit(a, 95, "t", null, "ca-" + a);
        eco.credit(b, 95, "t", null, "cb-" + b);
        GoalService goals = GoalService.getInstance();
        assertNotNull(goals.create("Test goal", 100));
        assertNull(goals.create("Second", 50)); // only one active at a time
        assertEquals(60, goals.contribute(a, 60, "ga-" + a).charged);
        GoalService.Outcome out = goals.contribute(b, 60, "gb-" + b);
        assertEquals(40, out.charged);
        assertTrue(out.reachedNow);
        assertEquals(60, eco.getBalance(b));
        assertNull(goals.contribute(a, 5, "gc-" + a)); // reached goals take no more
        assertEquals(2, goals.cancelCurrent());
        assertEquals(100, eco.getBalance(a));
        assertEquals(100, eco.getBalance(b));
    }

    @Test
    public void cosmeticPurchaseIsAtomicAndPermanentItemsStack() throws Exception {
        long u = newUser();
        CosmeticShop shop = CosmeticShop.getInstance();
        try {
            shop.buy(u, "badge-set", "i-" + u);
            fail();
        } catch (InsufficientFundsException expected) { }
        assertEquals(5, eco.getBalance(u));
        eco.credit(u, 100, "t", null, "c-" + u);
        assertNotNull(shop.buy(u, "badge-set", "i1-" + u));
        assertNull(shop.buy(u, "badge-set", "i2-" + u)); // already owned
        assertNotNull(shop.buy(u, "flair-day", "i3-" + u));
        assertNotNull(shop.buy(u, "flair-day", "i4-" + u)); // extends
        assertEquals(105 - 35 - 5 - 5, eco.getBalance(u));
        assertEquals(2, shop.owned(u).size());
        assertNull(shop.buy(u, "nope", "i5-" + u));
    }

    @Test
    public void wagerLimitsBlockOptOut() throws Exception {
        long u = newUser();
        assertNull(WagerLimits.check(u, 5));
        WagerLimits.setPaidPlay(u, false);
        assertNotNull(WagerLimits.check(u, 5));
        WagerLimits.setPaidPlay(u, true);
    }
}

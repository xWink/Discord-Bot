package integration;

import economy.EconomyService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WalletCapTest extends DbTestBase {

    @Test
    public void aWinPastTheMaximumBalanceCapsInsteadOfGoingNegative() throws Exception {
        EconomyService eco = EconomyService.getInstance();
        long u = newUser();
        eco.credit(u, 1_500_000_000, "test", "t", "cap-a-" + u);
        int after = eco.credit(u, 999_000_000, "test", "t", "cap-b-" + u);
        assertEquals(Integer.MAX_VALUE, after);
        assertTrue(eco.getBalance(u) > 0);
    }
}

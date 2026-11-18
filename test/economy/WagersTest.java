package economy;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WagersTest {

    @After
    public void restore() {
        Wagers.setCooldownMs(3000);
    }

    @Test
    public void secondGameInsideThePauseIsTurnedAway() {
        Wagers.setCooldownMs(60_000);
        assertNull(Wagers.cooldown(1));
        String wait = Wagers.cooldown(1);
        assertNotNull(wait);
        assertTrue(wait, wait.contains("next game"));
        assertNull("someone else is unaffected", Wagers.cooldown(2));
    }

    @Test
    public void zeroTurnsTheLimitOff() {
        Wagers.setCooldownMs(0);
        assertNull(Wagers.cooldown(1));
        assertNull(Wagers.cooldown(1));
    }
}

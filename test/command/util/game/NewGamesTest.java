package command.util.game;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NewGamesTest {

    // ----------------------------------------------------------------- roulette

    @Test
    public void rouletteBetsParseAndPay() {
        assertEquals(RouletteGame.Bet.Kind.RED, RouletteGame.parse("RED").kind);
        assertEquals(0, RouletteGame.parse("green").number);
        assertNull(RouletteGame.parse("37"));
        assertNull(RouletteGame.parse("purple"));
        assertEquals(35, RouletteGame.parse("17").payout());
        assertEquals(10, RouletteGame.net(RouletteGame.parse("red"), 1, 10));
        assertEquals(-10, RouletteGame.net(RouletteGame.parse("red"), 0, 10));
        assertEquals(-10, RouletteGame.net(RouletteGame.parse("odd"), 0, 10)); // zero beats every outside bet
        assertEquals(350, RouletteGame.net(RouletteGame.parse("17"), 17, 10));
    }

    @Test
    public void rouletteWheelHasEveryNumberOnce() {
        boolean[] seen = new boolean[37];
        for (int n : RouletteGame.WHEEL) seen[n] = true;
        for (boolean b : seen) assertTrue(b);
        assertEquals(37, RouletteGame.WHEEL.length);
        assertEquals(18, java.util.stream.IntStream.rangeClosed(1, 36).filter(RouletteGame::isRed).count());
    }

    @Test
    public void rouletteReturnsAboutNinetyThreePercent() {
        // every bet has the same house edge of 1/37; check an outside bet and a number exactly
        double red = 0, number = 0;
        for (int r = 0; r < 37; r++) {
            red += (RouletteGame.net(RouletteGame.parse("red"), r, 1000) + 1000) / 1000.0 / 37;
            number += (RouletteGame.net(RouletteGame.parse("7"), r, 1000) + 1000) / 1000.0 / 37;
        }
        assertEquals(36.0 / 37, number, 1e-9);
        assertEquals(36.0 / 37, red, 1e-9);
    }

    // -------------------------------------------------------------------- crash

    @Test
    public void crashTargetsParseAndPay() {
        assertEquals(250, CrashGame.parseTarget("2.5"));
        assertEquals(250, CrashGame.parseTarget("2.5x"));
        assertEquals(-1, CrashGame.parseTarget("1.0"));
        assertEquals(-1, CrashGame.parseTarget("abc"));
        assertEquals(10, CrashGame.net(250, 200, 10)); // crash at 2.50 beats a 2.00 target: win the stake
        assertEquals(-10, CrashGame.net(150, 200, 10));
        assertEquals("2.50x", CrashGame.show(250));
    }

    @Test
    public void crashChanceMatchesTheFormula() {
        Random random = new Random(7);
        int n = 200_000;
        int reached = 0;
        for (int i = 0; i < n; i++) if (CrashGame.wins(CrashGame.crashPoint(random), 200)) reached++;
        assertEquals(0.485, reached / (double) n, 0.01);
        assertTrue(CrashGame.crashPoint(new Random(1)) >= 100);
    }

    // -------------------------------------------------------------------- mines

    @Test
    public void minesMultiplierGrowsAndMatchesTheOdds() {
        assertEquals(1.0, MinesGame.multiplier(3, 0), 1e-9);
        assertEquals(0.97 * 16 / 13, MinesGame.multiplier(3, 1), 1e-9);
        assertTrue(MinesGame.multiplier(3, 5) > MinesGame.multiplier(3, 4));
        assertTrue(MinesGame.multiplier(8, 2) > MinesGame.multiplier(1, 2)); // more mines, bigger reward
    }

    @Test
    public void minesPlaysThroughSafeTilesAndAMine() {
        MinesGame game = new MinesGame(1, 100, "r", 3, new Random(3));
        assertEquals(3, game.getMines().size());
        int safe = 0;
        for (int tile = 0; tile < MinesGame.TILES; tile++) {
            if (game.getMines().contains(tile)) continue;
            assertEquals(MinesGame.Result.SAFE, game.pick(tile));
            safe++;
            assertEquals(MinesGame.Result.ALREADY, game.pick(tile));
        }
        assertTrue(game.allSafeFound());
        assertEquals(13, safe);
        assertTrue(game.netIfStopped() > 100);

        MinesGame boom = new MinesGame(1, 100, "r", 3, new Random(3));
        int mine = boom.getMines().iterator().next();
        assertEquals(MinesGame.Result.MINE, boom.pick(mine));
        assertEquals(mine, boom.getExploded());
        assertFalse(boom.allSafeFound());
    }

    // ------------------------------------------------------------------ scratch

    @Test
    public void scratchCardsNeverShowAnAccidentalSecondWin() {
        Random random = new Random(11);
        for (int i = 0; i < 20_000; i++) {
            ScratchGame.Card card = ScratchGame.deal(random);
            int[] count = new int[SlotsGame.Symbol.values().length];
            for (SlotsGame.Symbol s : card.cells) count[s.ordinal()]++;
            int triples = 0;
            for (int c : count) if (c >= 3) triples++;
            assertEquals(card.prize == null ? 0 : 1, triples);
            if (card.prize != null) assertEquals(3, count[card.prize.symbol.ordinal()]);
        }
    }

    @Test
    public void scratchReturnsAboutNinetyTwoPercent() {
        double expected = 0;
        for (ScratchGame.Prize p : ScratchGame.Prize.values()) expected += p.chance / 10_000.0 * p.tenths / 10.0;
        assertEquals(0.915, expected, 1e-9);
        Random random = new Random(5);
        long paid = 0;
        int n = 300_000;
        for (int i = 0; i < n; i++) paid += ScratchGame.payout(ScratchGame.deal(random), 100);
        assertEquals(0.915, paid / (n * 100.0), 0.03);
        assertNotNull(ScratchGame.deal(random));
    }
}

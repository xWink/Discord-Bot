package integration;

import economy.EconomyService;
import economy.SeasonService;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class SeasonIntegrationTest extends DbTestBase {

    private void scores(long u, int right, long when) throws Exception {
        for (int i = 0; i < right; i++) {
            exec("INSERT INTO arcade_scores (user, question_id, correct, elapsed_ms, week, played_at) VALUES ("
                    + u + ", 1, 1, 1000, 1, " + when + ")");
        }
    }

    @Test
    public void closingASeasonSnapshotsOnceAwardsBadgeAndRespectsOptOut() throws Exception {
        int season = 201501;
        long when = java.time.LocalDate.of(2015, 1, 15).atStartOfDay(java.time.ZoneId.of("America/New_York"))
                .toInstant().toEpochMilli();
        exec("DELETE FROM season_snapshots WHERE season = " + season);
        exec("DELETE FROM arcade_scores WHERE played_at BETWEEN " + (when - 20L * 86400000) + " AND " + (when + 20L * 86400000));
        long a = newUser(), b = newUser(), hidden = newUser();
        scores(a, 5, when);
        scores(b, 3, when);
        scores(hidden, 9, when);
        SeasonService.setOptOut(hidden, true);

        List<SeasonService.Row> live = SeasonService.live("arcade", season);
        assertEquals(a, live.get(0).user);
        assertFalse(live.stream().anyMatch(r -> r.user == hidden));

        Set<Long> winners = SeasonService.closeSeason(season);
        assertTrue(winners.contains(a));
        SeasonService.closeSeason(season); // idempotent
        assertEquals(live.size() > 10 ? 10 : live.size(), SeasonService.snapshot("arcade", season).size());
        assertEquals(1, SeasonService.snapshot("arcade", season).get(0).rank);
        assertTrue(SeasonService.currentWinners().contains(a));
        assertEquals(1, economy.CosmeticShop.getInstance().owned(a).size());
        SeasonService.setOptOut(hidden, false);
    }

    @Test
    public void everyBoardQueryRuns() throws Exception {
        for (String board : SeasonService.BOARDS) assertNotNull(SeasonService.live(board, SeasonService.currentSeason()));
    }

    @Test
    public void previousSeasonCloseRunsOnce() throws Exception {
        exec("DELETE FROM economy_settings WHERE name = 'season_snapshotted'");
        assertTrue(SeasonService.closePreviousIfDue());
        assertFalse(SeasonService.closePreviousIfDue());
    }
}

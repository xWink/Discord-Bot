package courses;

import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReminderTimeTest {

    /** Wednesday 18 November 2026, 10:00 on campus. */
    private static final ZonedDateTime NOW = ZonedDateTime.of(2026, 11, 18, 10, 0, 0, 0, ReminderTime.ZONE);

    private static ReminderTime.Result parse(String words) {
        return ReminderTime.parse(Arrays.asList(words.split(" ")), NOW);
    }

    private static ZonedDateTime when(ReminderTime.Result r) {
        return ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(r.dueMs), ReminderTime.ZONE);
    }

    @Test
    public void lengthsOfTime() {
        assertEquals(NOW.plusMinutes(10), when(parse("10m call mum")));
        assertEquals(1, parse("10m call mum").used);
        assertEquals(NOW.plusHours(1).plusMinutes(30), when(parse("1h30m x")));
        assertEquals(NOW.plusHours(2), when(parse("in 2 hours study")));
        assertEquals(3, parse("in 2 hours study").used);
        assertEquals(NOW.plusDays(1).plusHours(3), when(parse("1 day 3 hours x")));
        assertEquals(NOW.plusWeeks(2), when(parse("2w x")));
        assertEquals("a number that is not a unit is the text", null, parse("3 blind mice"));
    }

    @Test
    public void tomorrowTodayAndWeekdays() {
        assertEquals(NOW.toLocalDate().plusDays(1).atTime(9, 0).atZone(ReminderTime.ZONE), when(parse("tomorrow lab")));
        assertEquals(NOW.toLocalDate().plusDays(1).atTime(17, 30).atZone(ReminderTime.ZONE), when(parse("tomorrow 5:30pm lab")));
        assertEquals(NOW.toLocalDate().atTime(15, 0).atZone(ReminderTime.ZONE), when(parse("today at 3pm go")));
        // Friday after Wednesday the 18th is the 20th
        assertEquals(java.time.LocalDate.of(2026, 11, 20).atTime(17, 0).atZone(ReminderTime.ZONE), when(parse("friday 5pm lab")));
        assertEquals(java.time.LocalDate.of(2026, 11, 20).atTime(9, 0).atZone(ReminderTime.ZONE), when(parse("fri lab")));
        // Wednesday at 9 has passed, so the next one
        assertEquals(java.time.LocalDate.of(2026, 11, 25).atTime(9, 0).atZone(ReminderTime.ZONE), when(parse("wednesday x")));
    }

    @Test
    public void datesAndBareTimes() {
        assertEquals(java.time.LocalDate.of(2026, 12, 4).atTime(9, 0).atZone(ReminderTime.ZONE), when(parse("2026-12-04 drop")));
        assertEquals(java.time.LocalDate.of(2026, 12, 4).atTime(14, 15).atZone(ReminderTime.ZONE), when(parse("2026-12-04 14:15 drop")));
        assertEquals(NOW.toLocalDate().atTime(18, 0).atZone(ReminderTime.ZONE), when(parse("at 6pm dinner")));
        // 9am has passed today, so it means tomorrow
        assertEquals(NOW.toLocalDate().plusDays(1).atTime(9, 0).atZone(ReminderTime.ZONE), when(parse("at 9am gym")));
        assertEquals(NOW.toLocalDate().atTime(12, 0).atZone(ReminderTime.ZONE), when(parse("at noon lunch")));
    }

    @Test
    public void thingsThatAreNotATimeOrCannotBeUsed() {
        assertNull(parse("buy milk"));
        assertNull(parse("at"));
        assertEquals("That is not a real date.", parse("2026-02-31 x").error);
        assertEquals("That time has already passed.", parse("2026-11-01 x").error);
        assertTrue(parse("2030-01-01 x").error.contains("year"));
        assertNotNull(parse("10m x").dueMs);
    }
}

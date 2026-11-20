package courses;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DateCatalogTest {

    private final DateCatalog catalog = DateCatalog.of(Arrays.asList(
            "term\tdate\tdescription",
            "Fall Semester 2026\t2026-12-04\tLast day to drop F26 courses",
            "Fall Semester 2026\t2026-12-07\tExaminations commence.",
            "D.V.M. Fall Semester 2026\t2026-12-05\tVet rotation starts",
            "Fall Semester 2026\t2026-09-10\tClasses commence",
            "Fall Semester 2026\tnot a date\tbroken line",
            "Fall Semester 2026\t2026-12-04\tLast day for something else"));

    @Test
    public void upcomingIsSortedAndHidesTheVeterinaryCalendarUnlessAsked() {
        List<DateCatalog.Event> all = catalog.upcoming(LocalDate.of(2026, 11, 1), Collections.<String>emptyList(), false, 10);
        assertEquals(3, all.size());
        assertEquals(LocalDate.of(2026, 12, 4), all.get(0).date);
        assertEquals(4, catalog.upcoming(LocalDate.of(2026, 11, 1), Collections.<String>emptyList(), true, 10).size());
        assertEquals(1, catalog.upcoming(LocalDate.of(2026, 11, 1), Collections.<String>emptyList(), false, 1).size());
    }

    @Test
    public void wordsFilterAndOnGivesTheDaysEventsInOrder() {
        assertEquals(1, catalog.upcoming(LocalDate.of(2026, 1, 1), Arrays.asList("exam"), false, 10).size());
        assertEquals(1, catalog.upcoming(LocalDate.of(2026, 1, 1), Arrays.asList("drop", "f26"), false, 10).size());
        assertEquals(2, catalog.on(LocalDate.of(2026, 12, 4), false).size());
        assertEquals("Last day for something else", catalog.on(LocalDate.of(2026, 12, 4), false).get(1).description);
    }

    @Test
    public void theRealFileHasDatesWithRealWords() {
        DateCatalog real = DateCatalog.get();
        assertTrue("dates: " + real.size(), real.size() > 50);
        for (DateCatalog.Event e : real.upcoming(LocalDate.of(2000, 1, 1), Collections.<String>emptyList(), true, 1000)) {
            assertTrue(e.description, e.description.length() > 3);
            assertTrue(e.term, !e.term.isEmpty());
        }
    }
}

package courses;

import courses.Grades.Item;
import courses.Grades.Outcome;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GradesTest {

    private static final List<Item> HALF_DONE = Arrays.asList(
            new Item("lab1", 10, 80.0), new Item("lab2", 10, 90.0), new Item("midterm", 30, 70.0),
            new Item("labs3", 10, null), new Item("final", 40, null));

    @Test
    public void standingWeighsOnlyWhatIsMarked() {
        Grades.Standing s = Grades.standing(HALF_DONE);
        assertEquals(100, s.listedWeight, 1e-9);
        assertEquals(50, s.gradedWeight, 1e-9);
        assertEquals(8 + 9 + 21, s.earned, 1e-9);
        assertEquals(76.0, s.average, 1e-9);          // 38 of 50 points
        assertEquals(50, s.remaining, 1e-9);
        assertEquals(38, s.worst, 1e-9);
        assertEquals(88, s.best, 1e-9);
    }

    @Test
    public void nothingMarkedHasNoAverage() {
        Grades.Standing s = Grades.standing(Collections.singletonList(new Item("final", 100, null)));
        assertNull(s.average);
        assertEquals(100, s.best, 1e-9);
        assertEquals(0, s.worst, 1e-9);
    }

    @Test
    public void needWorksOutTheAverageOnWhatIsLeft() {
        Grades.Need n = Grades.need(HALF_DONE, 70);
        assertEquals(Outcome.POSSIBLE, n.outcome);
        assertEquals(64.0, n.average, 1e-9);           // (70 - 38) / 50
        assertEquals(50, n.remaining, 1e-9);
        assertEquals(Outcome.IMPOSSIBLE, Grades.need(HALF_DONE, 95).outcome);   // best is 88
        assertEquals(Outcome.SECURED, Grades.need(HALF_DONE, 35).outcome);       // already 38
        assertEquals(Outcome.POSSIBLE, Grades.need(HALF_DONE, 88).outcome);      // needs exactly full marks
        assertEquals(100.0, Grades.need(HALF_DONE, 88).average, 1e-6);
    }

    @Test
    public void everythingMarkedLeavesNothingToChange() {
        List<Item> done = Arrays.asList(new Item("a", 50, 60.0), new Item("b", 50, 80.0));
        assertEquals(Outcome.NOTHING_LEFT, Grades.need(done, 75).outcome);
        assertEquals(Outcome.SECURED, Grades.need(done, 70).outcome);
    }

    @Test
    public void weightNotListedCountsAsStillToCome() {
        // only 60% of the course is listed: the missing 40% is still to come
        List<Item> partial = Arrays.asList(new Item("a", 30, 80.0), new Item("b", 30, 60.0));
        Grades.Standing s = Grades.standing(partial);
        assertEquals(60, s.listedWeight, 1e-9);
        assertEquals(42, s.earned, 1e-9);
        assertEquals(40, s.remaining, 1e-9);
        assertEquals(Outcome.POSSIBLE, Grades.need(partial, 70).outcome);
        assertEquals(70.0, Grades.need(partial, 70).average, 1e-9); // 28 of 40
    }
}

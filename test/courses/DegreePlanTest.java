package courses;

import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import command.util.misc.Prerequisites;
import courses.DegreePlan.Offering;
import courses.DegreePlan.Placement;
import courses.DegreePlan.Plan;
import courses.DegreePlan.Season;
import courses.DegreePlan.Term;
import courses.DegreePlan.TermPlan;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DegreePlanTest {

    private final CourseCatalog catalog = CourseCatalog.get();
    private static final Term START = new Term(Season.WINTER, 2027);

    @Test
    public void termsFollowTheYearAndStartAfterTheCurrentOne() {
        assertEquals("Summer 2027", new Term(Season.WINTER, 2027).next().label());
        assertEquals("Fall 2027", new Term(Season.SUMMER, 2027).next().label());
        assertEquals("Winter 2028", new Term(Season.FALL, 2027).next().label());
        assertEquals("Winter 2027", Term.after(LocalDate.of(2026, 11, 18)).label());  // now is fall: next is winter
        assertEquals("Summer 2026", Term.after(LocalDate.of(2026, 2, 1)).label());
        assertEquals("Fall 2026", Term.after(LocalDate.of(2026, 7, 1)).label());
    }

    @Test
    public void offeredFollowsTheCatalogueText() {
        assertEquals(Offering.YES, DegreePlan.offered(catalog.find("CIS*2430"), Season.FALL));      // Fall Only
        assertEquals(Offering.NO, DegreePlan.offered(catalog.find("CIS*2430"), Season.WINTER));
        assertEquals(Offering.YES, DegreePlan.offered(catalog.find("CIS*3750"), Season.WINTER));    // Fall and Winter
        assertEquals(Offering.NO, DegreePlan.offered(catalog.find("CIS*3750"), Season.SUMMER));
        assertEquals(Offering.UNKNOWN, DegreePlan.offered(catalog.find("CIS*6020"), Season.FALL));  // Unspecified
    }

    @Test
    public void prerequisiteNeededPicksTheShortestWayThroughAnOr() {
        Prerequisites p = Prerequisites.parse("CIS*2520 , (CIS*2430 or ENGG*1420)");
        assertEquals(Arrays.asList("CIS*2520", "CIS*2430"), p.needed(new HashSet<String>()));
        assertEquals(Arrays.asList("CIS*2430"), p.needed(new HashSet<>(Arrays.asList("CIS*2520"))));
        assertTrue(p.needed(new HashSet<>(Arrays.asList("CIS*2520", "ENGG*1420"))).isEmpty());
        assertEquals(Arrays.asList("MATH*1200"), Prerequisites.parse("CIS*1300, MATH*1200").needed(new HashSet<>(Arrays.asList("CIS*1300"))));
        assertEquals(Arrays.asList("BUS*2090"), Prerequisites.parse("(1 of BUS*2090, HROB*2090, PSYC*1000)").needed(new HashSet<String>()));
        assertEquals(Arrays.asList("9.00 credits including"), Prerequisites.parse("9.00 credits including CIS*2750").unchecked());
    }

    @Test
    public void aRouteFromNothingRespectsTermsOrderAndTheCourseLimit() {
        Course target = catalog.find("CIS*3750");
        Plan plan = DegreePlan.plan(catalog, target, new HashSet<String>(), START, 3);
        assertTrue(plan.complete);
        assertTrue(plan.notes.stream().anyMatch(n -> n.contains("9.00 credits")));
        Map<String, Integer> termOf = new HashMap<>();
        for (int t = 0; t < plan.terms.size(); t++) {
            TermPlan tp = plan.terms.get(t);
            assertTrue("at most 3 a term", tp.courses.size() <= 3);
            for (Placement pl : tp.courses) {
                termOf.put(pl.course.code, t);
                if (!pl.termUncertain) assertTrue(pl.course.code + " in " + tp.term, DegreePlan.offered(pl.course, tp.term.season) == Offering.YES);
            }
        }
        // everything a course needs is in an earlier term, and the target is last
        for (TermPlan tp : plan.terms) {
            for (Placement pl : tp.courses) for (String need : pl.needs) {
                assertTrue(need + " before " + pl.course.code, termOf.get(need) < termOf.get(pl.course.code));
            }
        }
        assertEquals(plan.terms.size() - 1, (int) termOf.get("CIS*3750"));
        assertEquals(plan.terms.get(plan.terms.size() - 1).term.label(), plan.finish().label());
        assertTrue(plan.terms.size() >= 4);
    }

    @Test
    public void whatYouHaveFinishedShortensTheRoute() {
        Course target = catalog.find("CIS*3750");
        Plan scratch = DegreePlan.plan(catalog, target, new HashSet<String>(), START, 5);
        Set<String> done = Prerequisites.closure(catalog, new HashSet<>(Arrays.asList("CIS*1300", "CIS*2500", "CIS*1910", "CIS*2520", "CIS*2430")));
        Plan ahead = DegreePlan.plan(catalog, target, done, START, 5);
        assertTrue(ahead.complete);
        assertTrue(ahead.terms.size() < scratch.terms.size());
        // CIS*2750 is the only thing left: Winter, then CIS*3750 in a later term it is offered
        assertEquals("CIS*2750", ahead.terms.get(0).courses.get(0).course.code);
        assertEquals("CIS*3750", ahead.terms.get(ahead.terms.size() - 1).courses.get(0).course.code);
        // already done
        Plan finished = DegreePlan.plan(catalog, target, new HashSet<>(Arrays.asList("CIS*3750")), START, 5);
        assertTrue(finished.alreadyDone && finished.terms.isEmpty());
    }

    @Test
    public void everyCourseWithPrerequisitesGetsAPlanThatKeepsItsRules() {
        int checked = 0;
        for (String dept : catalog.departments()) {
            for (Course c : catalog.department(dept)) {
                if (c.prerequisiteCodes().isEmpty() || c.source.equals("graduate") || checked++ % 7 != 0) continue;
                Plan plan = DegreePlan.plan(catalog, c, new HashSet<String>(), START, 5);
                assertNotNull(c.code, plan);
                Map<String, Integer> termOf = new HashMap<>();
                for (int t = 0; t < plan.terms.size(); t++) {
                    assertTrue(c.code, plan.terms.get(t).courses.size() <= 5);
                    for (Placement pl : plan.terms.get(t).courses) {
                        assertFalse(c.code + " places " + pl.course.code + " twice", termOf.containsKey(pl.course.code));
                        termOf.put(pl.course.code, t);
                    }
                }
                for (TermPlan tp : plan.terms) for (Placement pl : tp.courses) for (String need : pl.needs) {
                    assertTrue(c.code + ": " + need + " before " + pl.course.code, termOf.get(need) < termOf.get(pl.course.code));
                }
                if (plan.complete) assertTrue(termOf.containsKey(c.code));
            }
        }
        assertTrue(checked > 100);
    }
}

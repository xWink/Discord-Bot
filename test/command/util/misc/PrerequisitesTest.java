package command.util.misc;

import command.util.misc.Prerequisites.Status;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PrerequisitesTest {

    private static Set<String> done(String... codes) {
        return new HashSet<>(Arrays.asList(codes));
    }

    @Test
    public void noPrerequisiteIsAlwaysMet() {
        assertEquals(Status.MET, Prerequisites.parse("NONE").check(done()));
        assertEquals(Status.MET, Prerequisites.parse("").check(done()));
        assertEquals(Status.MET, Prerequisites.parse(null).check(done()));
        assertTrue(Prerequisites.parse("NONE").isEmpty());
    }

    @Test
    public void orMeansEitherAndCommaMeansBoth() {
        Prerequisites p = Prerequisites.parse("Prerequisite(s): CIS*1300 or CIS*1500, MATH*1200");
        assertEquals(Status.UNMET, p.check(done()));
        assertEquals(Status.UNMET, p.check(done("CIS*1300")));
        assertEquals(Status.UNMET, p.check(done("MATH*1200")));
        assertEquals(Status.MET, p.check(done("CIS*1500", "MATH*1200")));
        assertEquals(Status.MET, p.check(done("CIS*1300", "MATH*1200")));
    }

    @Test
    public void bracketsGroupAndAndWorksLikeAComma() {
        Prerequisites p = Prerequisites.parse("(CIS*1300 or CIS*1500) and (MATH*1200 or MATH*1210)");
        assertEquals(Status.MET, p.check(done("CIS*1500", "MATH*1210")));
        assertEquals(Status.UNMET, p.check(done("CIS*1500")));
    }

    @Test
    public void whatCannotBeCheckedIsUnknownNotNo() {
        Prerequisites p = Prerequisites.parse("CIS*2500 or work experience in a related field");
        assertEquals(Status.MET, p.check(done("CIS*2500")));
        assertEquals(Status.UNKNOWN, p.check(done()));
        Prerequisites credits = Prerequisites.parse("4.00 credits including (ACCT*1240 or ACCT*2230)");
        assertEquals(Status.UNMET, credits.check(done()));        // no matching course: a definite no
        assertEquals(Status.UNKNOWN, credits.check(done("ACCT*1240"))); // the credits cannot be checked
    }

    @Test
    public void nOfAListMeansThatManyNotAll() {
        Prerequisites p = Prerequisites.parse("MCS*1000, (1 of BUS*2090, HROB*2090, PSYC*1000)");
        assertEquals(Status.UNMET, p.check(done("MCS*1000")));
        assertEquals(Status.MET, p.check(done("MCS*1000", "HROB*2090")));
        Prerequisites two = Prerequisites.parse("(2 of CIS*1300, CIS*1500, CIS*1910)");
        assertEquals(Status.UNMET, two.check(done("CIS*1300")));
        assertEquals(Status.MET, two.check(done("CIS*1300", "CIS*1910")));
    }

    @Test
    public void adviceAndTheCoursesOwnCodeAreNotRequirements() {
        Prerequisites p = Prerequisites.parse("MCS*1000. Although not required, it is recommended that students take "
                + "PSYC*1000 prior to MCS*2600.", "MCS*2600");
        assertEquals(Status.UNMET, p.check(done()));
        assertEquals(Status.MET, p.check(done("MCS*1000")));
    }

    @Test
    public void missingNamesWhatIsStillNeeded() {
        Prerequisites p = Prerequisites.parse("CIS*1300 or CIS*1500, MATH*1200");
        assertEquals("one of: CIS*1300 or CIS*1500, MATH*1200", p.missing(done()));
        assertEquals("MATH*1200", p.missing(done("CIS*1300")));
        assertEquals("", p.missing(done("CIS*1300", "MATH*1200")));
    }

    @Test
    public void courseThatCountsAsTheSameIsAccepted() {
        CourseCatalog catalog = CourseCatalog.get();
        Set<String> closure = Prerequisites.closure(catalog, done("BUS*3230"));
        assertTrue(closure.contains("ACCT*3230"));
    }

    @Test
    public void everyRealPrerequisiteLineReadsWithoutFailing() {
        CourseCatalog catalog = CourseCatalog.get();
        Set<String> nothing = done();
        int unmet = 0;
        for (String dept : catalog.departments()) {
            for (CourseCatalog.Course c : catalog.department(dept)) {
                Prerequisites p = Prerequisites.of(c);
                Status s = p.check(nothing);
                p.missing(nothing);
                // a course that names no course code anywhere must never be marked unmet
                if (c.prerequisiteCodes().isEmpty()) assertTrue(c.code + ": " + c.prerequisites, s != Status.UNMET);
                if (s == Status.UNMET) unmet++;
                // having every named course finished must never leave a plain course-code-only line unmet
                Set<String> all = new HashSet<>(c.prerequisiteCodes());
                assertTrue(c.code + ": " + c.prerequisites, p.check(all) != Status.UNMET);
            }
        }
        assertTrue("courses with a prerequisite: " + unmet, unmet > 1000);
    }
}

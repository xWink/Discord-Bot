package command.util.art;

import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CourseArtTest {

    private final CourseCatalog catalog = CourseCatalog.get();

    private static void save(String name, byte[] bytes) throws Exception {
        File dir = new File("build/course-art");
        dir.mkdirs();
        Files.write(new File(dir, name).toPath(), bytes);
        assertTrue(name + " is " + bytes.length + " bytes", bytes.length > 100 && bytes.length < 4L * 1024 * 1024);
    }

    @Test
    public void bannerDrawsForUndergraduateAndGraduateCourses() throws Exception {
        save("banner-cis3750.gif", CourseArt.banner(catalog.find("CIS*3750")));
        save("banner-grad.gif", CourseArt.banner(catalog.find("CIS*6020")));
    }

    @Test
    public void bannerSurvivesVeryLongTitlesAndNoFacts() throws Exception {
        Course longest = null;
        for (String dept : catalog.departments()) {
            for (Course c : catalog.department(dept)) if (longest == null || c.title.length() > longest.title.length()) longest = c;
        }
        assertNotNull(CourseArt.banner(longest));
    }

    @Test
    public void treeShowsPrerequisitesAndTheirPrerequisites() throws Exception {
        byte[] gif = CourseArt.prerequisiteTree(catalog, catalog.find("CIS*3750"));
        assertNotNull(gif);
        save("tree-cis3750.gif", gif);
        assertTrue(CourseArt.prerequisiteCount(catalog, catalog.find("CIS*3750")) >= 2);
    }

    @Test
    public void aCourseWithNothingNeededHasNoTree() throws Exception {
        assertNull(CourseArt.prerequisiteTree(catalog, catalog.find("CIS*1300")));
    }

    @Test
    public void everyCourseThatHasPrerequisitesDrawsWithoutFailing() throws Exception {
        int drawn = 0;
        for (String dept : catalog.departments()) {
            for (Course c : catalog.department(dept)) {
                if (c.prerequisiteCodes().isEmpty()) continue;
                if (drawn % 40 == 0) assertNotNull(c.code, CourseArt.prerequisiteTree(catalog, c)); // a sample: drawing all is slow
                else assertTrue(CourseArt.prerequisiteCount(catalog, c) >= 1);
                drawn++;
            }
        }
        assertTrue(drawn > 1000);
    }

    @Test
    public void ratingsChartGrowsAndHandlesAnEmptyBucket() throws Exception {
        java.lang.reflect.Constructor<courses.Ratings.Summary> ctor = courses.Ratings.Summary.class.getDeclaredConstructor(
                int.class, double.class, double.class);
        ctor.setAccessible(true);
        courses.Ratings.Summary s = ctor.newInstance(10, 3.2, 4.1);
        int[] d = {1, 2, 4, 2, 1};
        int[] w = {0, 0, 1, 4, 5};
        System.arraycopy(d, 0, s.difficultyCounts, 0, 5);
        System.arraycopy(w, 0, s.workloadCounts, 0, 5);
        save("ratings.gif", CourseArt.ratingsChart(catalog.find("CIS*3750"), s));
    }

    @Test
    public void treeShowsProgressForSomeoneWhoHasFinishedSomeCourses() throws Exception {
        Course target = catalog.find("CIS*3750");
        java.util.Set<String> done = command.util.misc.Prerequisites.closure(catalog,
                new java.util.HashSet<>(java.util.Arrays.asList("CIS*1300", "CIS*2500", "CIS*2520", "CIS*2430", "MATH*2000")));
        CourseArt.Progress p = CourseArt.progress(catalog, target, done);
        assertTrue("needed " + p.needed, p.needed >= 4);
        assertTrue("finished " + p.finished, p.finished >= 2);
        // CIS*2750 needs CIS*2520 and (CIS*2430 or ENGG*1420): both done, so it is ready now
        assertTrue(p.nextUp.stream().anyMatch(c -> c.code.equals("CIS*2750")));
        assertEquals(CourseArt.State.LOCKED, p.target); // CIS*3750 still needs CIS*2750 and credits
        save("tree-personal.gif", CourseArt.prerequisiteTree(catalog, target, done));

        // someone who has finished everything it asks for is told they can take it
        java.util.Set<String> all = command.util.misc.Prerequisites.closure(catalog, new java.util.HashSet<>(java.util.Arrays.asList(
                "CIS*2750")));
        CourseArt.Progress ready = CourseArt.progress(catalog, target, all);
        assertEquals("the credits cannot be checked, so a maybe", CourseArt.State.MAYBE, ready.target);
        // nothing finished: no personal colouring, same as before
        CourseArt.Progress none = CourseArt.progress(catalog, target, java.util.Collections.<String>emptySet());
        assertEquals(0, none.finished);
        assertTrue(none.nextUp.isEmpty());
        assertNotNull(CourseArt.prerequisiteTree(catalog, target, java.util.Collections.<String>emptySet()));
    }
}

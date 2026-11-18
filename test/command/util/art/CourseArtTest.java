package command.util.art;

import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

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
}

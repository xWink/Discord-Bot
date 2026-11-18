package command.util.misc;

import command.util.misc.CourseCatalog.Course;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CourseCatalogTest {

    private final CourseCatalog catalog = CourseCatalog.get();

    @Test
    public void everyCourseInTheFileParses() {
        assertTrue("courses: " + catalog.size(), catalog.size() > 3000);
        int graduate = 0;
        for (String dept : catalog.departments()) {
            for (Course c : catalog.department(dept)) {
                assertFalse(c.code, c.title.isEmpty());
                assertFalse(c.code, c.credits.isEmpty());
                assertFalse(c.code + " title still has markup: " + c.title, c.title.contains("[") || c.title.contains("<"));
                assertTrue(c.code, c.source.equals("undergraduate") || c.source.equals("graduate"));
                assertFalse(c.code + " has stray spaces in brackets", c.prerequisites.contains("( ") || c.prerequisites.contains(" )"));
                if (c.source.equals("graduate")) graduate++;
            }
        }
        assertTrue("graduate courses: " + graduate, graduate > 1000);
    }

    @Test
    public void aRowIsReadIntoItsFields() {
        Course a = catalog.find("ACCT*1220");
        assertEquals("Introductory Financial Accounting", a.title);
        assertEquals("Summer, Fall, and Winter", a.offered);
        assertEquals("0.50", a.credits);
        assertEquals("undergraduate", a.source);
        Course b = catalog.find("acct2290");
        assertEquals("Fall Only", b.offered);
        assertEquals("LEC: 3, LAB: 2", b.hours);
        assertEquals("4.00 credits including (ACCT*1240 or ACCT*2230)", b.prerequisites);
        assertEquals("Software for Legacy Systems", catalog.find("CIS*3190").title);
        assertEquals("graduate", catalog.find("CIS*6020").source);
        assertEquals(6, catalog.find("CIS*6020").level());
    }

    @Test
    public void equivalentAndCorequisiteCoursesAreKept() {
        assertEquals(java.util.Collections.singletonList("NUTR*3210"), catalog.find("ANSC*3120").corequisiteCodes());
        assertEquals(java.util.Collections.singletonList("BUS*3230"), catalog.find("ACCT*3230").equateCodes());
        boolean anyCoreq = false;
        for (String dept : catalog.departments()) for (Course c : catalog.department(dept)) anyCoreq |= !c.corequisites.isEmpty();
        assertTrue(anyCoreq);
    }

    @Test
    public void aFileWithFewerColumnsStillReads() {
        CourseCatalog small = CourseCatalog.of(Arrays.asList("code\ttitle\tcredits", "CIS*1300\tProgramming\t0.50",
                "garbage line", "CIS*1500\t\t0.50"));
        assertEquals(1, small.size()); // the line without a title is skipped
        assertEquals("", small.find("cis1300").prerequisites);
        assertEquals("0.50", small.find("cis1300").credits);
    }

    @Test
    public void codesAreFoundHoweverTheyAreWritten() {
        for (String s : new String[]{"cis1300", "CIS*1300", "cis 1300", "CIS 1300", " Cis*1300 "}) {
            assertNotNull(s, catalog.find(s));
            assertEquals("CIS*1300", catalog.find(s).code);
        }
        assertNull(catalog.find("cis9999"));
        assertNull(catalog.find("hello"));
    }

    @Test
    public void searchFindsByTitleWordsCodeStartAndDepartment() {
        List<Course> accounting = catalog.search("financial accounting", 5);
        assertEquals("ACCT*1220", accounting.get(0).code);
        assertTrue(accounting.size() <= 5);
        for (Course c : catalog.search("programming", 10)) {
            assertTrue(c.code + " " + c.title, c.title.toLowerCase().contains("programming")
                    || c.description.toLowerCase().contains("programming"));
        }
        List<Course> byPrefix = catalog.search("cis13", 5);
        assertFalse(byPrefix.isEmpty());
        assertTrue(byPrefix.get(0).code, byPrefix.get(0).code.startsWith("CIS*13"));
        assertEquals("CIS*1300", catalog.search("cis1300", 3).get(0).code);
        assertTrue(catalog.search("zzzzqq", 5).isEmpty());
        assertTrue(catalog.search("   ", 5).isEmpty());
        List<Course> dept = catalog.department("CIS");
        assertTrue(dept.size() > 20);
        for (int i = 1; i < dept.size(); i++) assertTrue(dept.get(i - 1).number <= dept.get(i).number);
        assertTrue(catalog.department("nonsense").isEmpty());
    }

    @Test
    public void prerequisiteCodesAreExtractedInOrderWithoutRepeats() {
        assertEquals(Arrays.asList("ACCT*1240", "ACCT*2230"), catalog.find("ACCT*2290").prerequisiteCodes());
        assertTrue(catalog.find("CIS*1300").prerequisiteCodes().isEmpty());
        for (String dept : catalog.departments()) {
            for (Course c : catalog.department(dept)) {
                assertFalse(c.code, c.prerequisiteCodes().contains(c.code));
            }
        }
    }

    @Test
    public void aBareNumberFindsThatCourseInEveryDepartmentAndDescriptionsNeverWin() {
        List<Course> found = catalog.match("4820", 10);
        assertEquals(6, found.size());
        assertEquals("CIS*4820", found.get(0).code); // ties are in code order
        for (Course c : found) assertEquals(4820, c.number);
        // STAT*6802 only mentions 4820 in its restrictions or text: it must not be among them
        assertFalse(found.stream().anyMatch(c -> c.code.equals("STAT*6802")));
        // match() ignores descriptions entirely, search() may use them but ranks them last
        assertTrue(catalog.match("zzzzqq", 5).isEmpty());
        for (Course c : catalog.match("generalized linear", 10)) assertTrue(c.title.toLowerCase().contains("generalized linear"));
    }
}

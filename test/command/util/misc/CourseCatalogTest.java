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
        assertTrue("courses: " + catalog.size(), catalog.size() > 1800);
        Set<String> offered = new HashSet<>(Arrays.asList("", "Fall and Winter", "Summer, Fall, and Winter",
                "Summer and Fall", "Winter and Summer", "Fall Only", "Winter Only", "Summer Only", "Unspecified"));
        for (String dept : catalog.departments()) {
            for (Course c : catalog.department(dept)) {
                assertFalse(c.code, c.title.isEmpty());
                assertFalse(c.code, c.credits.isEmpty());
                assertTrue(c.code + " offered: " + c.offered, offered.contains(c.offered));
                assertFalse(c.code + " title still has its tail: " + c.title, c.title.contains("["));
            }
        }
    }

    @Test
    public void aTitleLineIsSplitIntoItsParts() {
        Course a = catalog.find("ACCT*1220");
        assertEquals("Introductory Financial Accounting", a.title);
        assertEquals("Summer, Fall, and Winter", a.offered);
        assertEquals("LEC 3", a.hours);
        assertEquals("0.50", a.credits);
        Course b = catalog.find("acct2290");
        assertEquals("Fall Only", b.offered);
        assertEquals("LEC 3, LAB 2", b.hours);
        // no hours in brackets, and no semester named
        Course c = catalog.find("HUMN*3600");
        assertEquals("International Field Study", c.title);
        assertEquals("", c.offered);
        assertEquals("1.00", c.credits);
        // hours missing but a semester named
        assertEquals("Winter Only", catalog.find("CIS*3190").offered);
        assertEquals("Software for Legacy Systems", catalog.find("CIS*3190").title);
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
}

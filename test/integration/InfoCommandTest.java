package integration;

import command.commands.misc.Info;
import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The course lookup against the real catalogue. Needs no database. */
public class InfoCommandTest {

    @Before
    public void drawHere() {
        command.ui.Reveal.setInline(true); // draw on this thread so the reply is there to check
        Info.useRatings(code -> null);      // no database in these tests
    }

    @After
    public void drawElsewhere() {
        command.ui.Reveal.setInline(false);
    }

    private static FakeDiscord ask(String content) {
        FakeDiscord f = FakeDiscord.say(1, "Student", content, main.Server.BOTS_CHANNEL_ID);
        Info info = new Info();
        assertTrue(content, info.keyMatches(content));
        info.start(f.event);
        return f;
    }

    @Test
    public void aCodeShowsTheFullEntryHoweverItIsTyped() {
        for (String s : new String[]{"!info cis1300", "!INFO CIS*1300", "!info cis 1300"}) {
            FakeDiscord f = ask(s);
            assertEquals(s, 1, f.embeds.size());
            MessageEmbed e = f.embeds.get(0);
            assertTrue(e.getTitle(), e.getTitle().contains("CIS") && e.getTitle().contains("1300"));
            assertTrue(e.getFields().stream().anyMatch(x -> "About".equals(x.getName())));
            assertTrue(e.getFields().stream().anyMatch(x -> "Prerequisites".equals(x.getName())));
        }
    }

    @Test
    public void wordsFromTheTitleSuggestCourses() {
        FakeDiscord f = ask("!info financial accounting");
        assertEquals(1, f.embeds.size());
        assertTrue(f.embeds.get(0).getDescription(), f.embeds.get(0).getDescription().contains("ACCT"));
    }

    @Test
    public void aDepartmentListsItsCourses() {
        FakeDiscord f = ask("!info cis");
        assertEquals(1, f.embeds.size());
        assertTrue(f.embeds.get(0).getTitle().startsWith("CIS courses"));
    }

    @Test
    public void nothingFoundOrNothingAskedGetsHelp() {
        assertTrue(ask("!info zzzzqq").said().contains("could not find"));
        assertTrue(ask("!info").said().contains("!info <course>"));
    }

    @Test
    public void noCourseCanBreakDiscordsLimits() throws Exception {
        // the catalogue has descriptions longer than an embed field allows; every entry must still build and send
        java.lang.reflect.Method embed = Info.class.getDeclaredMethod("courseEmbed", Course.class);
        java.lang.reflect.Method buttons = Info.class.getDeclaredMethod("courseButtons", Course.class);
        embed.setAccessible(true);
        buttons.setAccessible(true);
        CourseCatalog catalog = CourseCatalog.get();
        int checked = 0;
        for (String dept : catalog.departments()) {
            for (Course c : catalog.department(dept)) {
                MessageEmbed e = ((net.dv8tion.jda.api.EmbedBuilder) embed.invoke(null, c)).build();
                for (MessageEmbed.Field field : e.getFields()) assertTrue(c.code, field.getValue().length() <= 1024);
                assertFalse(c.code, ((java.util.List<?>) buttons.invoke(null, c)).isEmpty());
                checked++;
            }
        }
        assertTrue(checked > 1800);
    }

    @Test
    public void aCourseEntryComesWithAnAnimatedBanner() {
        FakeDiscord f = ask("!info cis3750");
        assertEquals(1, f.files.size());
        assertTrue(f.files.get(0).length > 1000);
        assertEquals("attachment://course.gif", f.embeds.get(0).getImage().getUrl());
        assertTrue(f.embeds.get(0).getFooter().getText(), f.embeds.get(0).getFooter().getText().contains("3000-level"));
    }

    @Test
    public void graduateCoursesAreFoundToo() {
        FakeDiscord f = ask("!info cis6020");
        assertEquals(1, f.embeds.size());
        assertTrue(f.embeds.get(0).getFooter().getText().startsWith("Graduate"));
    }

    @Test
    public void relatedCoursesGetTheirOwnFields() {
        FakeDiscord f = ask("!info acct3230");
        assertTrue(f.embeds.get(0).getFields().stream().anyMatch(x -> x.getName().startsWith("Counts as the same")));
        FakeDiscord g = ask("!info ansc3120");
        assertTrue(g.embeds.get(0).getFields().stream().anyMatch(x -> x.getName().startsWith("Co-requisites")));
    }

    @Test
    public void prereqDrawsATreeOrSaysThereIsNone() {
        command.commands.misc.Prereq prereq = new command.commands.misc.Prereq();
        FakeDiscord f = FakeDiscord.say(1, "Student", "!prereq cis3750", main.Server.BOTS_CHANNEL_ID);
        prereq.start(f.event);
        assertEquals(1, f.files.size());
        assertTrue(f.said(), f.said().contains("CIS\\*3750"));
        FakeDiscord none = FakeDiscord.say(1, "Student", "!prereq cis1300", main.Server.BOTS_CHANNEL_ID);
        prereq.start(none.event);
        assertTrue(none.said(), none.said().contains("no prerequisite"));
        assertTrue(none.files.isEmpty());
        FakeDiscord help = FakeDiscord.say(1, "Student", "!prereq", main.Server.BOTS_CHANNEL_ID);
        prereq.start(help.event);
        assertTrue(help.said().contains("!prereq <course>"));
    }

    @Test
    public void prereqAndReviewsAlsoAskWhichCourseForAnAmbiguousNumber() {
        FakeDiscord f = FakeDiscord.say(1, "Student", "!prereq 4820", main.Server.BOTS_CHANNEL_ID);
        new command.commands.misc.Prereq().start(f.event);
        assertTrue(f.embeds.get(0).getTitle().startsWith("Which course"));
        assertTrue(f.files.isEmpty());
    }
}

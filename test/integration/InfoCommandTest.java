package integration;

import command.commands.misc.Info;
import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The course lookup against the real catalogue. Needs no database. */
public class InfoCommandTest {

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
}

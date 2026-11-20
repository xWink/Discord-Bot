package integration;

import command.Command;
import command.commands.course.GradeCommands;
import courses.Grades;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The grade tracker: marks are saved, shown only by direct message, and never posted in the channel. */
public class GradeIntegrationTest extends DbTestBase {

    private static FakeDiscord say(Command c, long user, String content) {
        FakeDiscord f = FakeDiscord.say(user, "Student", content, main.Server.BOTS_CHANNEL_ID);
        assertTrue(content, c.keyMatches(content));
        c.start(f.event);
        return f;
    }

    /** What was said in the channel itself: no direct messages. */
    private static String channel(FakeDiscord f) {
        StringBuilder sb = new StringBuilder();
        for (String t : f.texts) sb.append(t).append('\n');
        for (net.dv8tion.jda.api.entities.MessageEmbed e : f.embeds) sb.append(FakeDiscord.flatten(e)).append('\n');
        return sb.toString();
    }

    private static String dm(FakeDiscord f) {
        StringBuilder sb = new StringBuilder();
        for (net.dv8tion.jda.api.entities.MessageEmbed e : f.directMessages) {
            sb.append(e.getTitle()).append('\n').append(e.getDescription()).append('\n');
            for (net.dv8tion.jda.api.entities.MessageEmbed.Field field : e.getFields()) sb.append(field.getName()).append(' ').append(field.getValue()).append('\n');
        }
        return sb.toString();
    }

    @Test
    public void marksAreSavedAndOnlyEverSentByDirectMessage() throws Exception {
        long u = newUser();
        GradeCommands.Record grade = new GradeCommands.Record();
        FakeDiscord first = say(grade, u, "!grade cis1300 lab1 10 85");
        assertTrue(channel(first), channel(first).contains("Saved lab1") && channel(first).contains("DMs"));
        assertFalse("the mark is not posted in the channel", channel(first).contains("85"));
        assertTrue(dm(first), dm(first).contains("85.0%"));

        say(grade, u, "!grade cis 1300 midterm 30 70");
        say(grade, u, "!grade cis1300 final 60");
        assertEquals(3, Grades.items(u, "CIS*1300").size());
        FakeDiscord shown = say(new GradeCommands.Show(), u, "!grades cis1300");
        assertTrue(dm(shown), dm(shown).contains("Average on marked work") && dm(shown).contains("not marked"));
        assertFalse(channel(shown).contains("%"));

        // replacing an item keeps one row; a mark can be added later
        say(grade, u, "!grade cis1300 final 60 78");
        assertEquals(3, Grades.items(u, "CIS*1300").size());
        FakeDiscord need = say(new GradeCommands.NeedCommand(), u, "!need cis1300 80");
        assertTrue(dm(need), dm(need).contains("secured") || dm(need).contains("average of") || dm(need).contains("Even full marks") || dm(need).contains("short of"));
        assertFalse(channel(need).contains("%"));

        FakeDiscord all = say(new GradeCommands.Show(), u, "!grades");
        assertTrue(dm(all), dm(all).contains("CIS\\*1300"));
    }

    @Test
    public void badInputIsRefusedAndItemsCanBeRemoved() throws Exception {
        long u = newUser();
        GradeCommands.Record grade = new GradeCommands.Record();
        assertTrue(say(grade, u, "!grade").said().contains("!grade <course>"));
        assertTrue(say(grade, u, "!grade zzz9999 a 10").said().contains("do not know"));
        assertTrue(say(grade, u, "!grade cis1300 lab1 0").said().contains("weight"));
        assertTrue(say(grade, u, "!grade cis1300 lab1 10 -5").said().contains("mark"));
        assertTrue(say(grade, u, "!grade cis1300 lab1 ten").said().contains("weight"));
        assertTrue(Grades.items(u, "CIS*1300").isEmpty());
        say(grade, u, "!grade cis1300 lab1 10 85");
        say(grade, u, "!grade cis1300 lab2 10 90");
        assertTrue(say(grade, u, "!grade remove cis1300 lab1").said().contains("Removed"));
        assertTrue(say(grade, u, "!grade remove cis1300 lab1").said().contains("no item"));
        assertTrue(say(grade, u, "!grade clear cis1300").said().contains("Cleared 1"));
        assertTrue(say(new GradeCommands.NeedCommand(), u, "!need cis1300 70").said().contains("no marks"));
        assertTrue(say(new GradeCommands.NeedCommand(), u, "!need cis1300 abc").said().contains("!need <course> <target%>"));
        assertTrue(say(new GradeCommands.Show(), u, "!grades").said().contains("no marks recorded"));
    }

    @Test
    public void aCourseHoldsAtMostTwentyItems() throws Exception {
        long u = newUser();
        GradeCommands.Record grade = new GradeCommands.Record();
        for (int i = 0; i < Grades.MAX_ITEMS_PER_COURSE; i++) say(grade, u, "!grade cis1300 item" + i + " 1 50");
        assertTrue(say(grade, u, "!grade cis1300 onetoomany 1 50").said().contains("already have"));
        assertEquals(Grades.MAX_ITEMS_PER_COURSE, Grades.items(u, "CIS*1300").size());
    }
}

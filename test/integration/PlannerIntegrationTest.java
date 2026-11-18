package integration;

import command.Command;
import command.commands.course.Planner;
import courses.CompletedStore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The finished-courses list, {@code !next} and {@code !whynot}, against the real catalogue. */
public class PlannerIntegrationTest extends DbTestBase {

    private static FakeDiscord say(Command c, long user, String content) {
        FakeDiscord f = FakeDiscord.say(user, "Student", content, main.Server.BOTS_CHANNEL_ID);
        assertTrue(content, c.keyMatches(content));
        c.start(f.event);
        return f;
    }

    @Test
    public void completedListAddsRemovesAndNamesWhatItDoesNotKnow() throws Exception {
        long u = newUser();
        Planner.Completed completed = new Planner.Completed();
        assertTrue(say(completed, u, "!completed").said().contains("not listed"));
        String added = say(completed, u, "!completed add cis1300, CIS*1200 cis 2500 nope1234 cis1300").said();
        assertTrue(added, added.contains("Added 3"));
        assertTrue(added, added.contains("NOPE\\*1234"));
        assertEquals(3, CompletedStore.list(u).size());
        assertTrue(CompletedStore.list(u).contains("CIS*2500"));
        assertTrue(say(completed, u, "!completed add cis1300").said().contains("Added 0"));
        assertTrue(say(completed, u, "!completed remove cis1200").said().contains("Removed 1"));
        assertEquals(2, CompletedStore.list(u).size());
        FakeDiscord list = say(completed, u, "!completed list");
        assertTrue(list.embeds.get(0).getTitle().contains("(2)"));
        assertTrue(say(completed, u, "!completed clear").said().contains("Cleared 2"));
        assertTrue(CompletedStore.list(u).isEmpty());
    }

    @Test
    public void nextShowsWhatTheFinishedCoursesUnlock() throws Exception {
        long u = newUser();
        assertTrue(say(new Planner.Next(), u, "!next").said().contains("finished first"));
        CompletedStore.add(u, java.util.Arrays.asList("CIS*1300"));
        FakeDiscord f = say(new Planner.Next(), u, "!next");
        assertEquals(1, f.embeds.size());
        String all = f.embeds.get(0).getFields().get(0).getValue();
        assertTrue(all, all.contains("CIS\\*2500")); // CIS*2500 asks only for CIS*1300
        assertFalse("not a course that also needs something else", all.contains("CIS\\*3750"));
        // a department and level filter
        FakeDiscord dept = say(new Planner.Next(), u, "!next cis 2");
        assertTrue(dept.embeds.get(0).getTitle().contains("CIS"));
        assertTrue(say(new Planner.Next(), u, "!next banana").said().contains("do not know"));
    }

    @Test
    public void whyNotNamesWhatIsMissingAndSaysWhenYouAreReady() throws Exception {
        long u = newUser();
        Planner.WhyNot why = new Planner.WhyNot();
        FakeDiscord no = say(why, u, "!whynot cis2500");
        String description = no.embeds.get(0).getDescription();
        assertTrue(description, description.contains("Not yet") && description.contains("CIS\\*1300"));
        CompletedStore.add(u, java.util.Arrays.asList("CIS*1300"));
        assertTrue(say(why, u, "!whynot cis2500").embeds.get(0).getDescription().contains("meet the listed"));
        // 9.00 credits including CIS*2750: the credits cannot be checked, so it is a maybe, not a yes
        CompletedStore.add(u, java.util.Arrays.asList("CIS*2750"));
        assertTrue(say(why, u, "!whynot cis3750").embeds.get(0).getDescription().contains("cannot"));
        assertTrue(say(why, u, "!whynot cis1300").embeds.get(0).getDescription().contains("already"));
        assertTrue(say(why, u, "!whynot zzzzqq").said().contains("could not find"));
    }
}

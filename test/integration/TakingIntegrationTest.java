package integration;

import command.Command;
import command.commands.course.Taking;
import courses.CompletedStore;
import courses.TakingStore;
import economy.ProfilePrefs;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TakingIntegrationTest extends DbTestBase {

    private static FakeDiscord say(Command c, long user, String content) {
        FakeDiscord f = FakeDiscord.say(user, "Student", content, main.Server.BOTS_CHANNEL_ID);
        assertTrue(content, c.keyMatches(content));
        c.start(f.event);
        return f;
    }

    @Test
    public void takingListsAddsRemovesAndMovesToFinishedWhenDone() throws Exception {
        long u = newUser();
        Taking.Manage taking = new Taking.Manage();
        assertTrue(say(taking, u, "!taking").said().contains("not listed"));
        String added = say(taking, u, "!taking add cis2750 math1200 nope1234").said();
        assertTrue(added, added.contains("Added 2") && added.contains("NOPE\\*1234"));
        assertEquals(2, TakingStore.list(u).size());
        assertTrue(say(taking, u, "!taking list").embeds.get(0).getTitle().contains("(2)"));

        assertTrue(say(taking, u, "!taking done cis2750").said().contains("Moved 1"));
        assertTrue(CompletedStore.list(u).contains("CIS*2750"));
        assertFalse(TakingStore.list(u).contains("CIS*2750"));
        assertTrue(say(taking, u, "!taking done cis2750").said().contains("None of those"));
        assertTrue(say(taking, u, "!taking remove math1200").said().contains("Removed 1"));
        TakingStore.add(u, java.util.Arrays.asList("CIS*3750", "CIS*2430"));
        assertTrue(say(taking, u, "!taking done all").said().contains("Moved 2"));
        assertTrue(TakingStore.list(u).isEmpty());
        assertEquals(3, CompletedStore.list(u).size());
        assertTrue(say(taking, u, "!taking banana").said().contains("!taking add"));
    }

    @Test
    public void onlyPeopleWhoShareAppearAsClassmates() throws Exception {
        // the database is shared between runs, so count against what is already there
        TakingStore.Classmates base = TakingStore.classmates("CIS*3190", 1000);
        long visible = newUser(), hidden = newUser();
        TakingStore.add(visible, java.util.Arrays.asList("CIS*3190"));
        TakingStore.add(hidden, java.util.Arrays.asList("CIS*3190"));
        String none = say(new Taking.Classmates(), newUser(), "!classmates cis3190").embeds.get(0).getDescription();
        assertTrue(none, none.contains((base.total + 2) + " people are taking this now"));
        assertFalse(none.contains("<@" + visible + ">"));

        say(new Taking.Manage(), visible, "!taking share on");
        assertTrue(ProfilePrefs.isSharingClasses(visible));
        String shown = say(new Taking.Classmates(), newUser(), "!classmates cis3190").embeds.get(0).getDescription();
        assertTrue(shown, shown.contains("<@" + visible + ">"));
        assertFalse("the other person is still hidden", shown.contains("<@" + hidden + ">"));
        assertTrue(say(new Taking.Manage(), visible, "!taking share").said().contains("**on**"));
        say(new Taking.Manage(), visible, "!taking share off");
        assertFalse(say(new Taking.Classmates(), newUser(), "!classmates cis3190").embeds.get(0).getDescription().contains("<@" + visible + ">"));
        TakingStore.remove(visible, java.util.Arrays.asList("CIS*3190"));
        TakingStore.remove(hidden, java.util.Arrays.asList("CIS*3190"));
    }

    @Test
    public void aPersonCanListOnlyTwelveCurrentCourses() throws Exception {
        long u = newUser();
        java.util.List<String> many = new java.util.ArrayList<>();
        for (int i = 0; i < 15; i++) many.add("TST*" + (1000 + i));
        assertEquals(TakingStore.MAX, TakingStore.add(u, many));
        assertEquals(TakingStore.MAX, TakingStore.list(u).size());
    }
}

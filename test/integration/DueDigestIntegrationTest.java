package integration;

import command.Command;
import command.commands.course.DigestCommand;
import command.commands.course.Due;
import courses.Digest;
import courses.DigestStore;
import courses.DueItems;
import courses.ReminderTime;
import courses.Reminders;
import org.junit.Test;

import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DueDigestIntegrationTest extends DbTestBase {

    private static FakeDiscord say(Command c, long user, String content) {
        FakeDiscord f = FakeDiscord.say(user, "Student", content, main.Server.BOTS_CHANNEL_ID);
        assertTrue(content, c.keyMatches(content));
        c.start(f.event);
        return f;
    }

    @Test
    public void dueItemsAreAddedListedTickedOffAndTakeTheirReminderWithThem() throws Exception {
        long u = newUser();
        Due due = new Due();
        assertTrue(say(due, u, "!due").said().contains("Nothing is due"));
        String added = say(due, u, "!due add cis2500 lab3 in 3 days").said();
        assertTrue(added, added.contains("Added **#") && added.contains("the day before"));
        assertEquals(1, Reminders.pending(u).size());
        assertTrue(Reminders.pending(u).get(0).text, Reminders.pending(u).get(0).text.contains("Due tomorrow: CIS*2500 lab3"));

        say(due, u, "!due add cis 2500 \"final project\" 2026-12-20 17:00");
        FakeDiscord list = say(due, u, "!due");
        assertTrue(list.embeds.get(0).getTitle().contains("(2)"));
        assertTrue(list.embeds.get(0).getDescription().contains("final project"));
        assertTrue(say(due, u, "!due cis2500").embeds.get(0).getTitle().contains("CIS"));

        long id = DueItems.open(u, null).get(0).id;
        assertTrue(say(due, u, "!due done " + id).said().contains("Done:"));
        assertTrue("its reminder went with it", Reminders.pending(u).stream().noneMatch(r -> r.text.contains("lab3")));
        assertTrue(say(due, u, "!due done " + id).said().contains("no open item"));
        long other = DueItems.open(u, null).get(0).id;
        assertTrue(say(due, u, "!due remove " + other).said().contains("Removed"));
        assertTrue(DueItems.open(u, null).isEmpty());
    }

    @Test
    public void dueRejectsBadInputAndPastTimes() throws Exception {
        long u = newUser();
        Due due = new Due();
        assertTrue(say(due, u, "!due add").said().contains("!due add <course>"));
        assertTrue(say(due, u, "!due add zzz9999 lab friday").said().contains("!due add <course>"));
        assertTrue(say(due, u, "!due add cis2500 lab nonsense words").said().contains("could not read"));
        assertTrue(say(due, u, "!due add cis2500 lab 2020-01-01").said().contains("already passed"));
        assertTrue(DueItems.open(u, null).isEmpty());
        // too soon for a day-before reminder: an hour before instead
        String soon = say(due, u, "!due add cis2500 quiz in 3 hours").said();
        assertTrue(soon, soon.contains("an hour before"));
        String sooner = say(due, u, "!due add cis2500 quiz2 in 30 minutes").said();
        assertTrue(sooner, sooner.contains("too soon"));
    }

    @Test
    public void digestIsClaimedOncePerDayAndOnlyBuiltWhenThereIsSomethingToSay() throws Exception {
        long u = newUser();
        ZonedDateTime now = ZonedDateTime.now(ReminderTime.ZONE);
        DigestStore.set(u, true, 0); // midnight, so it is always due
        long today = now.toLocalDate().toEpochDay();
        assertTrue(DigestStore.claimDue(today, 1439).contains(u));
        assertTrue("sent once", !DigestStore.claimDue(today, 1439).contains(u));
        assertTrue("and again tomorrow", DigestStore.claimDue(today + 1, 1439).contains(u));
        DigestStore.set(u, false, 0);
        assertTrue(!DigestStore.claimDue(today + 2, 1439).contains(u));
        assertEquals(-1, DigestStore.minute(u));

        long id = DueItems.add(u, "CIS*2500", "lab3", System.currentTimeMillis() + 20 * 3600 * 1000, 0);
        Reminders.create(u, 1, "call mum", System.currentTimeMillis() + 3 * 3600 * 1000, false);
        net.dv8tion.jda.api.EmbedBuilder digest = Digest.build(u, now);
        assertNotNull(digest);
        String all = digest.build().getFields().stream().map(f -> f.getName() + " " + f.getValue()).reduce("", (a, b) -> a + b + "\n");
        assertTrue(all, all.contains("lab3") && all.contains("call mum"));
        assertTrue(id > 0);
    }

    @Test
    public void digestCommandSetsAndShowsTheTimeAndNowSendsADirectMessage() throws Exception {
        long u = newUser();
        DigestCommand digest = new DigestCommand();
        assertTrue(say(digest, u, "!digest").said().contains("**off**"));
        assertTrue(say(digest, u, "!digest on 7:30").said().contains("7:30 am"));
        assertEquals(7 * 60 + 30, DigestStore.minute(u));
        assertTrue(say(digest, u, "!digest on 6pm").said().contains("6:00 pm"));
        assertTrue(say(digest, u, "!digest on").said().contains("8:00 am"));
        assertTrue(say(digest, u, "!digest on 25:99").said().contains("could not read"));
        assertTrue(say(digest, u, "!digest").said().contains("**on**"));
        DueItems.add(u, "CIS*2500", "lab9", System.currentTimeMillis() + 3600 * 1000, 0);
        FakeDiscord now = say(digest, u, "!digest now");
        assertEquals(1, now.directMessages.size());
        assertTrue(now.directMessages.get(0).getTitle().startsWith("Good morning"));
        assertTrue(say(digest, u, "!digest off").said().contains("no more"));
        assertEquals(-1, DigestStore.minute(u));
    }
}

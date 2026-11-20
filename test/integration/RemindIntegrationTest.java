package integration;

import command.Command;
import command.commands.course.Remind;
import courses.Reminders;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Reminders and the calendar dates, against a real database. */
public class RemindIntegrationTest extends DbTestBase {

    private static FakeDiscord say(Command c, long user, String content) {
        FakeDiscord f = FakeDiscord.say(user, "Student", content, main.Server.BOTS_CHANNEL_ID);
        assertTrue(content + " does not match", c.keyMatches(content));
        c.start(f.event);
        return f;
    }

    @Test
    public void aReminderIsSavedListedCancelledAndSentOnce() throws Exception {
        long u = newUser();
        Remind.Create remind = new Remind.Create();
        String made = say(remind, u, "!remind 2h call mum @everyone").said();
        assertTrue(made, made.contains("I will message you"));
        assertFalse("the text cannot ping everyone", made.contains("@everyone"));
        List<Reminders.Reminder> pending = Reminders.pending(u);
        assertEquals(1, pending.size());
        assertEquals("call mum @everyone", pending.get(0).text);
        assertTrue(pending.get(0).due > System.currentTimeMillis() + 119 * 60 * 1000);

        FakeDiscord list = say(new Remind.All(), u, "!reminders");
        assertTrue(list.embeds.get(0).getTitle().contains("(1)"));

        // nothing is due yet, then it is: claimed once, and not again
        assertTrue(Reminders.claimDue(System.currentTimeMillis(), 50).stream().noneMatch(r -> r.user == u));
        long soon = Reminders.create(u, 1, "now please", System.currentTimeMillis() - 1000, false);
        List<Reminders.Reminder> due = Reminders.claimDue(System.currentTimeMillis(), 50);
        assertTrue(due.stream().anyMatch(r -> r.id == soon));
        assertTrue(Reminders.claimDue(System.currentTimeMillis(), 50).stream().noneMatch(r -> r.id == soon));

        long id = pending.get(0).id;
        assertTrue(say(remind, u, "!remind cancel " + id).said().contains("Cancelled"));
        assertTrue(say(remind, u, "!remind cancel " + id).said().contains("no waiting"));
        assertTrue(Reminders.pending(u).isEmpty());
    }

    @Test
    public void badInputGetsHelpAndOnlyTwentyFiveCanWait() throws Exception {
        long u = newUser();
        Remind.Create remind = new Remind.Create();
        assertTrue(say(remind, u, "!remind").said().contains("!remind <when> <what>"));
        assertTrue(say(remind, u, "!remind buy milk").said().contains("could not read a time"));
        assertTrue(say(remind, u, "!remind 2h").said().contains("What should I remind you about"));
        assertTrue(say(remind, u, "!remind 2020-01-01 x").said().contains("already passed"));
        for (int i = 0; i < Reminders.MAX_PENDING; i++) assertTrue(Reminders.create(u, 1, "r" + i, System.currentTimeMillis() + 60_000, false) > 0);
        assertTrue(say(remind, u, "!remind 3h one more").said().contains("already have"));
    }

    @Test
    public void datesListUpcomingEventsWithReminderButtons() throws Exception {
        long u = newUser();
        FakeDiscord f = say(new Remind.Dates(), u, "!dates 5");
        // the calendar file may have run out of future dates, in which case it says so instead
        if (f.embeds.isEmpty()) {
            assertTrue(f.said(), f.said().contains("no upcoming"));
            return;
        }
        assertTrue(f.embeds.get(0).getTitle().contains("calendar"));
        assertTrue(f.embeds.get(0).getDescription().contains("**"));
    }

    @Test
    public void aCalendarDateBecomesAReminderTheDayBefore() throws Exception {
        long u = newUser();
        courses.DateCatalog catalog = courses.DateCatalog.get();
        java.time.LocalDate today = java.time.LocalDate.now(courses.ReminderTime.ZONE);
        List<courses.DateCatalog.Event> upcoming = catalog.upcoming(today.plusDays(3), java.util.Collections.<String>emptyList(), false, 1);
        if (upcoming.isEmpty()) return; // the calendar file has run out of future dates
        courses.DateCatalog.Event e = upcoming.get(0);
        int nth = catalog.on(e.date, true).indexOf(e) + 1;
        say(new Remind.Create(), u, "!remind date " + e.date + " " + nth);
        List<Reminders.Reminder> mine = Reminders.pending(u);
        assertEquals(1, mine.size());
        assertTrue(mine.get(0).text, mine.get(0).text.startsWith("Tomorrow: "));
    }
}

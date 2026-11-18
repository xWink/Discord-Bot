package integration;

import command.Command;
import command.commands.course.Reviews;
import courses.Ratings;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Course ratings, against a real database. */
public class ReviewsIntegrationTest extends DbTestBase {

    @Before
    public void drawHere() {
        command.ui.Reveal.setInline(true);
    }

    private static FakeDiscord say(Command c, long user, String content) {
        FakeDiscord f = FakeDiscord.say(user, "Student", content, main.Server.BOTS_CHANNEL_ID);
        assertTrue(content + " does not match", c.keyMatches(content));
        c.start(f.event);
        return f;
    }

    /** A course code no other test has rated, so averages here are only these tests'. */
    private static String freshCourse() {
        return "TST*" + (1000 + (int) (Math.random() * 8999));
    }

    @Test
    public void ratingsAverageOnlyOnceThreePeopleHaveRatedAndRatingAgainReplaces() throws Exception {
        String code = freshCourse();
        long a = newUser(), b = newUser(), c = newUser();
        assertTrue(Ratings.rate(a, code, 2, 3, "easy marking"));
        assertFalse("rating again replaces", Ratings.rate(a, code, 4, 3, "actually harder"));
        Ratings.Summary one = Ratings.summary(code);
        assertEquals(1, one.count);
        assertEquals(4.0, one.difficulty, 1e-9);
        assertFalse(one.enoughToShow());
        Ratings.rate(b, code, 2, 5, null);
        Ratings.rate(c, code, 3, 1, "ok");
        Ratings.Summary s = Ratings.summary(code);
        assertEquals(3, s.count);
        assertTrue(s.enoughToShow());
        assertEquals(3.0, s.difficulty, 1e-9);
        assertEquals(3.0, s.workload, 1e-9);
        assertEquals(1, s.difficultyCounts[3]);   // one 4
        assertEquals(1, s.workloadCounts[4]);     // one 5
        assertNull(Ratings.summary(freshCourse()));
        assertEquals("a rating with no words has no comment", 2, Ratings.comments(code, 10).size());
        assertTrue(Reviews.line(code, s).contains("3.0"));
        assertTrue(Reviews.line(code, one).contains("1 rating so far"));
        assertTrue(Reviews.line(code, null).contains("No ratings yet"));
    }

    @Test
    public void rateCommandChecksItsInputAndAcceptsSpacedCourseCodes() throws Exception {
        long u = newUser();
        Reviews.Rate rate = new Reviews.Rate();
        assertTrue(say(rate, u, "!rate").said().contains("!rate <course>"));
        assertTrue(say(rate, u, "!rate zzz9999 3 3").said().contains("do not know"));
        assertTrue(say(rate, u, "!rate cis1300 6 3").said().contains("1 to 5"));
        assertTrue(say(rate, u, "!rate cis1300 three 3").said().contains("!rate <course>"));
        String longComment = new String(new char[Ratings.MAX_COMMENT + 1]).replace('\0', 'x');
        assertTrue(say(rate, u, "!rate cis1300 3 3 " + longComment).said().contains("under"));
        String ok = say(rate, u, "!rate cis 1300 3 4 fair marking, heavy labs @everyone").said();
        assertTrue(ok, ok.contains("Thanks! Rated"));
        List<Ratings.Review> comments = Ratings.comments("CIS*1300", 20);
        assertTrue(comments.stream().anyMatch(r -> r.comment.startsWith("fair marking, heavy labs")));
        assertTrue("mentions are defanged", comments.stream().noneMatch(r -> r.comment.contains("@everyone")));
        assertTrue(say(rate, u, "!rate cis1300 2 2").said().contains("Updated your rating"));
        assertTrue(say(rate, u, "!rate remove cis1300").said().contains("Removed"));
        assertTrue(say(rate, u, "!rate remove cis1300").said().contains("no rating"));
    }

    @Test
    public void reviewsShowAChartAndTheLatestCommentsWithReportButtons() throws Exception {
        String code = "CIS*3750";
        for (int i = 0; i < 3; i++) Ratings.rate(newUser(), code, 3 + i % 2, 4, "comment " + i);
        FakeDiscord f = say(new Reviews.Show(), newUser(), "!reviews cis3750");
        assertEquals(1, f.files.size());
        assertEquals("attachment://ratings.gif", f.embeds.get(0).getImage().getUrl());
        assertTrue(f.embeds.get(0).getFields().get(0).getValue().contains("comment"));
        assertTrue(say(new Reviews.Show(), newUser(), "!reviews zzzz9999").said().contains("could not find"));
        assertNull(Ratings.summary("ACCT*1220")); // no test rates this one
        assertTrue(say(new Reviews.Show(), newUser(), "!reviews acct1220").said().contains("Nobody has rated"));
    }

    @Test
    public void threeDifferentReportsHideACommentAndOwnersCannotReportThemselves() throws Exception {
        String code = freshCourse();
        long author = newUser();
        Ratings.rate(author, code, 3, 3, "rude words");
        long id = Ratings.comments(code, 1).get(0).id;
        assertEquals(Ratings.Reported.OWN, Ratings.report(author, id));
        long r1 = newUser(), r2 = newUser(), r3 = newUser();
        assertEquals(Ratings.Reported.OK, Ratings.report(r1, id));
        assertEquals(Ratings.Reported.ALREADY, Ratings.report(r1, id));
        assertEquals(Ratings.Reported.OK, Ratings.report(r2, id));
        assertEquals(1, Ratings.comments(code, 5).size());
        assertEquals(Ratings.Reported.HIDDEN, Ratings.report(r3, id));
        assertTrue(Ratings.comments(code, 5).isEmpty());
        assertEquals("the scores still count", 1, Ratings.summary(code).count);
        assertEquals(Ratings.Reported.MISSING, Ratings.report(newUser(), id));
        // rating again after being hidden starts clean
        Ratings.rate(author, code, 3, 3, "better words");
        assertEquals(1, Ratings.comments(code, 5).size());
    }

    @Test
    public void reportCommandAndModeratorHide() throws Exception {
        String code = freshCourse();
        long author = newUser();
        Ratings.rate(author, code, 2, 2, "spam spam");
        long id = Ratings.comments(code, 1).get(0).id;
        assertTrue(say(new Reviews.Report(), newUser(), "!reportreview " + id).said().contains("noted"));
        assertTrue(say(new Reviews.Report(), newUser(), "!reportreview 99999999").said().contains("no visible"));
        assertTrue(say(new Reviews.Report(), newUser(), "!reportreview abc").said().contains("!reportreview <number>"));
        // an ordinary member cannot hide; the owner can
        say(new Reviews.Hide(), newUser(), "!review hide " + id);
        assertFalse(Ratings.comments(code, 5).isEmpty());
        FakeDiscord owner = FakeDiscord.sayAsOwner(newUser(), "Owner", "!review hide " + id, main.Server.BOTS_CHANNEL_ID);
        new Reviews.Hide().start(owner.event);
        assertTrue(owner.said(), owner.said().contains("Hidden review"));
        assertTrue(Ratings.comments(code, 5).isEmpty());
        assertNotNull(Ratings.summary(code));
    }

    @Test
    public void moderatorRemovalTakesTheRatingOutOfTheAveragesAndStaysOutWhenTheyRateAgain() throws Exception {
        String code = freshCourse();
        long spammer = newUser();
        Ratings.rate(newUser(), code, 2, 2, "fair");
        Ratings.rate(newUser(), code, 4, 4, "fair too");
        Ratings.rate(spammer, code, 5, 5, "spam spam spam");
        assertEquals(3, Ratings.summary(code).count);
        assertEquals(11.0 / 3, Ratings.summary(code).difficulty, 1e-9);
        long id = Ratings.comments(code, 1).get(0).id; // the newest: the spam

        // hiding the comment keeps the scores
        FakeDiscord hide = FakeDiscord.sayAsOwner(newUser(), "Owner", "!review hide " + id, main.Server.BOTS_CHANNEL_ID);
        new Reviews.Hide().start(hide.event);
        assertTrue(hide.said(), hide.said().contains("scores still count"));
        assertEquals(3, Ratings.summary(code).count);

        // removing takes them out
        FakeDiscord remove = FakeDiscord.sayAsOwner(newUser(), "Owner", "!review remove " + id, main.Server.BOTS_CHANNEL_ID);
        assertTrue(new Reviews.Hide().keyMatches("!review remove " + id));
        new Reviews.Hide().start(remove.event);
        assertTrue(remove.said(), remove.said().contains("no longer count"));
        Ratings.Summary s = Ratings.summary(code);
        assertEquals(2, s.count);
        assertEquals(3.0, s.difficulty, 1e-9);
        assertEquals(3.0, s.workload, 1e-9);

        // rating again changes the words but the scores stay out
        Ratings.rate(spammer, code, 5, 5, "I promise to be nice");
        assertEquals(2, Ratings.summary(code).count);
        assertEquals(1, Ratings.comments(code, 10).stream().filter(r -> r.comment.contains("nice")).count());

        // an ordinary member can do neither
        FakeDiscord member = FakeDiscord.say(newUser(), "Member", "!review remove " + id, main.Server.BOTS_CHANNEL_ID);
        new Reviews.Hide().start(member.event);
        assertEquals(2, Ratings.summary(code).count);
        assertTrue(member.said().isEmpty());
    }
}

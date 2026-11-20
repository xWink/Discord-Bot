package integration;

import command.commands.course.PlanCommand;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** {@code !plan} against the real catalogue; needs no database. */
public class PlanCommandTest {

    @SuppressWarnings("unchecked")
    private static Set<String>[] lists(String[] finished, String[] taking) {
        return new Set[]{new HashSet<>(Arrays.asList(finished)), new HashSet<>(Arrays.asList(taking))};
    }

    @Before
    public void drawHere() {
        command.ui.Reveal.setInline(true);
    }

    @After
    public void restore() {
        command.ui.Reveal.setInline(false);
        PlanCommand.useSource(user -> lists(new String[0], new String[0]));
    }

    private static FakeDiscord ask(String content) {
        FakeDiscord f = FakeDiscord.say(1, "Student", content, main.Server.BOTS_CHANNEL_ID);
        PlanCommand plan = new PlanCommand();
        assertTrue(plan.keyMatches(content));
        plan.start(f.event);
        return f;
    }

    @Test
    public void aPlanComesAsAnAnimatedRouteWithTheSameThingInText() {
        PlanCommand.useSource(user -> lists(new String[]{"CIS*1300", "CIS*2500"}, new String[]{"CIS*1910"}));
        FakeDiscord f = ask("!plan cis3750 3");
        assertEquals(1, f.files.size());
        assertTrue(f.said(), f.said().contains("Earliest finish") && f.said().contains("CIS\\*3750") && f.said().contains("Assuming you finish CIS\\*1910"));
        assertTrue(f.said().contains("9.00 credits"));
    }

    @Test
    public void noListsMeansStartingFromNothingAndSaysSo() {
        PlanCommand.useSource(user -> lists(new String[0], new String[0]));
        String said = ask("!plan cis3750").said();
        assertTrue(said, said.contains("not listed any courses"));
    }

    @Test
    public void aFinishedCourseAndBadInputAreHandled() {
        PlanCommand.useSource(user -> lists(new String[]{"CIS*3750"}, new String[0]));
        assertTrue(ask("!plan cis3750").said().contains("already finished"));
        assertTrue(ask("!plan").said().contains("!plan <course>"));
        assertTrue(ask("!plan zzzzqq").said().contains("could not find"));
        assertTrue(ask("!plan 4820").embeds.get(0).getTitle().startsWith("Which course"));
    }

    @Test
    public void withMotionOffTheAnswerIsJustTheText() {
        PlanCommand.useSource(user -> lists(new String[]{"CIS*1300"}, new String[0]));
        command.ui.Motion.useSource(user -> true);
        command.ui.Motion.begin(1);
        try {
            FakeDiscord f = ask("!plan cis2750");
            assertTrue(f.files.isEmpty());
            assertTrue(f.said(), f.said().contains("Earliest finish"));
        } finally {
            command.ui.Motion.end();
            command.ui.Motion.useSource(user -> false);
        }
    }
}

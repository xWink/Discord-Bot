package integration;

import command.commands.course.Compare;
import command.commands.course.PlanCommand;
import command.commands.misc.Info;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** {@code !compare} against the real catalogue; needs no database. */
public class CompareCommandTest {

    @SuppressWarnings("unchecked")
    private static Set<String>[] lists(String[] finished, String[] taking) {
        return new Set[]{new HashSet<>(Arrays.asList(finished)), new HashSet<>(Arrays.asList(taking))};
    }

    @Before
    public void noDatabase() {
        Info.useRatings(code -> null);
        PlanCommand.useSource(user -> lists(new String[]{"CIS*1300", "CIS*2500"}, new String[]{"CIS*2750"}));
    }

    @After
    public void restore() {
        PlanCommand.useSource(user -> lists(new String[0], new String[0]));
    }

    private static FakeDiscord ask(String content) {
        FakeDiscord f = FakeDiscord.say(1, "Student", content, main.Server.BOTS_CHANNEL_ID);
        Compare compare = new Compare();
        assertTrue(compare.keyMatches(content));
        compare.start(f.event);
        return f;
    }

    @Test
    public void twoCoursesSideBySideWithWhereYouStandOnEach() {
        FakeDiscord f = ask("!compare cis2500 cis2520");
        assertEquals(1, f.embeds.size());
        assertEquals("CIS\\*2500 vs CIS\\*2520", f.embeds.get(0).getTitle());
        assertEquals(2, f.embeds.get(0).getFields().size());
        String first = f.embeds.get(0).getFields().get(0).getValue();
        String second = f.embeds.get(0).getFields().get(1).getValue();
        assertTrue(first, first.contains("finished"));                    // CIS*2500 is on the finished list
        assertTrue(second, second.contains("**Runs** Fall Only") && second.contains("Needs"));
        assertTrue(second, second.contains("ready to take") || second.contains("not yet"));
        assertTrue(second, second.contains("not enough ratings"));
    }

    @Test
    public void threeCoursesAndTheTakingStatusAndTheLimit() {
        FakeDiscord f = ask("!compare cis2750, cis3750 vs cis2430 cis1300");   // a fourth is dropped
        assertEquals(3, f.embeds.get(0).getFields().size());
        assertTrue(f.embeds.get(0).getFields().get(0).getValue().contains("taking now"));
        String third = f.embeds.get(0).getFields().get(2).getValue();
        assertTrue(third, third.contains("ready to take") || third.contains("finished"));
    }

    @Test
    public void notEnoughCoursesOrUnknownOnesGetHelp() {
        assertTrue(ask("!compare cis2500").said().contains("!compare <course> <course>"));
        assertTrue(ask("!compare").said().contains("!compare <course> <course>"));
        String said = ask("!compare cis2500 zzz9999").said();
        assertTrue(said, said.contains("!compare <course> <course>") && said.contains("ZZZ\\*9999"));
    }
}

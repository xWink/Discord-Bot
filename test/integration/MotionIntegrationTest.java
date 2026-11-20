package integration;

import command.commands.admin.BotStatus;
import command.commands.misc.Flip;
import command.commands.misc.MotionCommand;
import command.ui.Motion;
import economy.ProfilePrefs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MotionIntegrationTest extends DbTestBase {

    @Before
    public void setUp() {
        economy.Wagers.setCooldownMs(0);
        command.ui.Reveal.setInline(true);
    }

    @After
    public void tearDown() {
        Motion.end();
        command.ui.Reveal.setInline(false);
    }

    @Test
    public void motionOffIsSavedAndThenEveryAnimationBecomesText() throws Exception {
        long u = newUser();
        MotionCommand motion = new MotionCommand();
        FakeDiscord status = FakeDiscord.say(u, "P", "!motion", main.Server.BOTS_CHANNEL_ID);
        motion.start(status.event);
        assertTrue(status.said(), status.said().contains("**on**"));

        FakeDiscord off = FakeDiscord.say(u, "P", "!motion off", main.Server.BOTS_CHANNEL_ID);
        motion.start(off.event);
        assertTrue(off.said(), off.said().contains("**off**"));
        assertTrue(ProfilePrefs.isReducedMotion(u));

        // a flip for this person: a plain answer, no GIF
        Motion.begin(u);
        FakeDiscord flip = FakeDiscord.say(u, "P", "!flip", main.Server.BOTS_CHANNEL_ID);
        new Flip().start(flip.event);
        Motion.end();
        assertTrue(flip.said(), flip.said().contains("Heads!") || flip.said().contains("Tails!"));
        assertTrue(flip.files.isEmpty());

        FakeDiscord on = FakeDiscord.say(u, "P", "!motion on", main.Server.BOTS_CHANNEL_ID);
        motion.start(on.event);
        assertFalse(ProfilePrefs.isReducedMotion(u));
        Motion.begin(u);
        FakeDiscord again = FakeDiscord.say(u, "P", "!flip", main.Server.BOTS_CHANNEL_ID);
        new Flip().start(again.event);
        Motion.end();
        assertTrue("the GIF is back", again.files.size() == 1);
    }

    @Test
    public void botStatusIsForModeratorsOnly() {
        FakeDiscord member = FakeDiscord.say(newUser(), "M", "!botstatus", main.Server.BOTS_CHANNEL_ID);
        new BotStatus().start(member.event);
        assertTrue(member.embeds.isEmpty());
        FakeDiscord owner = FakeDiscord.sayAsOwner(newUser(), "O", "!botstatus", main.Server.BOTS_CHANNEL_ID);
        new BotStatus().start(owner.event);
        assertTrue(owner.embeds.get(0).getTitle().equals("Bot status"));
        String all = owner.embeds.get(0).getFields().stream().map(f -> f.getName()).collect(java.util.stream.Collectors.joining(","));
        assertTrue(all, all.contains("Animations") && all.contains("Memory") && all.contains("Recent errors"));
    }
}

package command.ui;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MotionStatsTest {

    @After
    public void restore() {
        Motion.end();
        Motion.useSource(user -> false);
        Reveal.setInline(false);
    }

    @Test
    public void motionOffSkipsTheDrawingAndSendsTheResultAsText() {
        Motion.useSource(user -> user == 5);
        Reveal.setInline(true);
        MessageChannel channel = mock(MessageChannel.class);
        MessageCreateAction action = mock(MessageCreateAction.class, RETURNS_SELF);
        when(channel.sendMessage(any(CharSequence.class))).thenReturn(action);
        AtomicInteger drawn = new AtomicInteger();

        Motion.begin(5);
        assertTrue(Motion.reduced());
        Reveal.render(channel, "x.gif", "wait", () -> {
            drawn.incrementAndGet();
            return new byte[]{1};
        }, "You won!", new ArrayList<ItemComponent>(), 0);
        assertEquals("nothing was drawn", 0, drawn.get());
        verify(channel).sendMessage("You won!");
        verify(channel, never()).sendFiles(any(net.dv8tion.jda.api.utils.FileUpload[].class));

        // asynchronous lookups fall back to their plain version
        AtomicInteger plain = new AtomicInteger();
        Reveal.async(() -> {
            drawn.incrementAndGet();
            return new byte[]{1};
        }, bytes -> plain.addAndGet(100), plain::incrementAndGet);
        assertEquals(1, plain.get());
        assertEquals(0, drawn.get());
        Motion.end();

        // someone else, or nobody, still gets the animation
        Motion.begin(6);
        assertFalse(Motion.reduced());
        Motion.end();
        assertFalse("outside a command nobody is reduced", Motion.reduced());
    }

    @Test
    public void statsCountCommandsRendersAndKeepTheLastFiveErrors() {
        Stats.command("!slots");
        Stats.command("!slots");
        Stats.command("!info");
        Stats.render(100);
        Stats.render(300);
        Stats.renderFailed();
        for (int i = 0; i < 7; i++) Stats.error("!x", new IllegalStateException("boom " + i));
        Stats.Snapshot s = Stats.snapshot();
        assertTrue(s.commandsRun >= 3);
        assertTrue(s.topCommands.get(0), s.topCommands.get(0).startsWith("!slots"));
        assertTrue(s.renders >= 2);
        assertTrue(s.renderMaxMs >= 300);
        assertEquals(5, s.recentErrors.size());
        assertTrue(s.recentErrors.get(0), s.recentErrors.get(0).contains("boom 6")); // newest first
        assertEquals("2d 3h 4m", Stats.duration(((2 * 24 + 3) * 60 + 4) * 60_000L));
        assertEquals("0m", Stats.duration(1000));
    }
}

package command.ui;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RevealTest {

    @After
    public void restore() {
        Reveal.setInline(false);
    }

    @Test
    public void drawingHappensOffTheCallingThread() throws Exception {
        MessageChannel channel = mock(MessageChannel.class);
        MessageCreateAction action = mock(MessageCreateAction.class, RETURNS_SELF);
        when(channel.sendFiles(any(net.dv8tion.jda.api.utils.FileUpload[].class))).thenReturn(action);
        CountDownLatch drawn = new CountDownLatch(1);
        AtomicReference<String> thread = new AtomicReference<>();
        Reveal.render(channel, "x.gif", "wait", () -> {
            thread.set(Thread.currentThread().getName());
            drawn.countDown();
            return new byte[]{1, 2, 3};
        }, "result", new ArrayList<ItemComponent>(), 0);
        assertTrue(drawn.await(5, TimeUnit.SECONDS));
        assertEquals("game-art", thread.get());
        assertNotEquals(Thread.currentThread().getName(), thread.get());
    }

    @Test
    public void aDrawingFailureStillGivesThePlayerTheirResult() throws Exception {
        Reveal.setInline(true);
        MessageChannel channel = mock(MessageChannel.class);
        MessageCreateAction action = mock(MessageCreateAction.class, RETURNS_SELF);
        when(channel.sendMessage(any(CharSequence.class))).thenReturn(action);
        Reveal.render(channel, "x.gif", "wait", () -> {
            throw new java.io.IOException("no fonts");
        }, "You won 5 GC!", new ArrayList<ItemComponent>(), 0);
        verify(channel).sendMessage("You won 5 GC!");
        verify(action).queue();
    }
}

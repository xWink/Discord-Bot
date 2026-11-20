package command.ui;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

import net.dv8tion.jda.api.utils.FileUpload;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Sends an animation with a placeholder, then swaps in the result text and buttons once the animation has played,
 * so the words never give the outcome away early. If the edit fails the result is sent as a new message.
 */
public final class Reveal {

    /** Drawing a GIF takes a few hundred milliseconds, so it happens here and never on Discord's event thread. */
    private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(40), r -> {
                Thread t = new Thread(r, "game-art");
                t.setDaemon(true);
                return t;
            });

    private static volatile boolean inline;

    static {
        POOL.allowCoreThreadTimeOut(true);
    }

    private Reveal() { }

    /** @return how many drawings are waiting for a worker */
    public static int queued() {
        return POOL.getQueue().size();
    }

    /** @return how many workers are drawing right now */
    public static int drawing() {
        return POOL.getActiveCount();
    }

    /** Draw on the calling thread instead of the pool, so tests can check the result straight away. */
    public static void setInline(boolean inline) {
        Reveal.inline = inline;
    }

    /**
     * Draws the animation on a worker thread, sends it with a placeholder, then swaps in the result once it has played
     * (see {@link #send}). The game is already settled when this is called, so if drawing fails or the workers are
     * backed up the player still gets the result, as plain text.
     *
     * @param job draws the file
     */
    public static void render(MessageChannel channel, String fileName, String placeholder, Callable<byte[]> job,
                              String result, List<ItemComponent> buttons, long delayMs) {
        if (Motion.reduced()) { // the person turned animations off: the result, as text
            Stats.reduced();
            plain(channel, result, buttons);
            return;
        }
        final command.util.art.Theme theme = command.util.art.Theme.current(); // the worker draws in the asker's palette
        Runnable task = () -> {
            byte[] bytes;
            long started = System.currentTimeMillis();
            try {
                bytes = inTheme(theme, job);
                Stats.render(System.currentTimeMillis() - started);
            } catch (Exception | OutOfMemoryError e) {
                e.printStackTrace();
                Stats.renderFailed();
                plain(channel, result, buttons);
                return;
            }
            send(channel.sendFiles(FileUpload.fromData(bytes, fileName)).setContent(placeholder), channel, result,
                    buttons, delayMs);
        };
        if (inline) {
            task.run();
            return;
        }
        try {
            POOL.execute(task);
        } catch (RejectedExecutionException e) {
            Stats.renderRejected();
            plain(channel, result, buttons);
        }
    }

    /**
     * Draws on a worker thread, then hands the bytes to {@code ok} (also on that thread). If drawing fails or the
     * workers are backed up, {@code fallback} runs instead, so the caller always answers.
     */
    public static void async(Callable<byte[]> job, java.util.function.Consumer<byte[]> ok, Runnable fallback) {
        if (Motion.reduced()) { // the person turned animations off: no picture
            Stats.reduced();
            fallback.run();
            return;
        }
        final command.util.art.Theme theme = command.util.art.Theme.current(); // the worker draws in the asker's palette
        Runnable task = () -> {
            byte[] bytes;
            long started = System.currentTimeMillis();
            try {
                bytes = inTheme(theme, job);
                Stats.render(System.currentTimeMillis() - started);
            } catch (Exception | OutOfMemoryError e) {
                e.printStackTrace();
                Stats.renderFailed();
                fallback.run();
                return;
            }
            ok.accept(bytes);
        };
        if (inline) {
            task.run();
            return;
        }
        try {
            POOL.execute(task);
        } catch (RejectedExecutionException e) {
            Stats.renderRejected();
            fallback.run();
        }
    }

    private static byte[] inTheme(command.util.art.Theme theme, Callable<byte[]> job) throws Exception {
        command.util.art.Theme.use(theme);
        try {
            return job.call();
        } finally {
            if (!inline) command.util.art.Theme.clear();
        }
    }

    private static void plain(MessageChannel channel, String result, List<ItemComponent> buttons) {
        MessageCreateAction action = channel.sendMessage(result);
        if (!buttons.isEmpty()) action.setComponents(ActionRow.of(buttons));
        action.queue();
    }

    /**
     * @param action the message holding the animation, with the placeholder as its content
     * @param channel where to send the result if the edit fails
     * @param result the text that replaces the placeholder
     * @param buttons buttons to show with the result (may be empty)
     * @param delayMs how long the animation takes
     */
    public static void send(MessageCreateAction action, MessageChannel channel, String result,
                            List<ItemComponent> buttons, long delayMs) {
        action.queue(message -> {
            net.dv8tion.jda.api.requests.restaction.MessageEditAction edit = message.editMessage(result);
            if (!buttons.isEmpty()) edit.setComponents(ActionRow.of(buttons));
            edit.queueAfter(delayMs, TimeUnit.MILLISECONDS, ok -> { },
                    err -> channel.sendMessage(result).queue());
        });
    }
}

package command;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Timer;
import java.util.TimerTask;

public abstract class ResponseCommand extends Command {

    /**
     * Initializes the command's key.
     *
     * @param theKey the command's key
     * @param isGlobal whether the command can be used globally
     */
    public ResponseCommand(String theKey, boolean isGlobal) {
        super(theKey, isGlobal);
    }

    /**
     * Handles response from user who initiated command in same channel by calling respond method.
     * The respond method is implemented in the child classes and will vary based on its purpose.
     * ResponseHandler object lasts 5 minutes.
     */
    protected class ResponseHandler extends ListenerAdapter {
        private final long channelId, authorId;
        private JDA jda;
        private ExpireTask expireTask;
        private Timer timer;
        private ResponseHandler handler;

        public ResponseHandler(long channelId, long authorId, JDA jda) {
            handler = this;
            this.channelId = channelId;
            this.authorId = authorId;
            this.jda = jda;
            jda.addEventListener(handler);
            expireTask = new ExpireTask();
            timer = new Timer();
            timer.schedule(expireTask, 1000 * 60 * 5);
        }

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event.getAuthor().getIdLong() == authorId && event.getChannel().getIdLong() == channelId)
                respond(event);
        }

        public void expire() {
            expireTask.run();
            timer.cancel();
        }

        private class ExpireTask extends TimerTask {
            @Override
            public void run() {
                jda.removeEventListener(handler);
            }
        }
    }

    /**
     * How a ResponseCommand object handles a response from the user
     * @param event a MessageReceivedEvent that triggered the response
     */
    public abstract void respond(MessageReceivedEvent event);
}

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

    protected class ResponseHandler extends ListenerAdapter {
        private final long channelId, authorId;

        public ResponseHandler(long channelId, long authorId, JDA jda) {
            this.channelId = channelId;
            this.authorId = authorId;
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    jda.removeEventListener(this);
                }
            }, 1000 * 60 * 5);
        }

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event.getAuthor().getIdLong() == authorId && event.getChannel().getIdLong() == channelId)
                respond(event);
        }
    }

    public abstract void respond(MessageReceivedEvent event);
}

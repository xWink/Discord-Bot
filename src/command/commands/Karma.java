package command.commands;

import command.Command;
import database.connectors.KarmaConnector;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Karma extends Command {

    private KarmaConnector kc;

    /**
     * Initializes the command's key to "!karma".
     */
    public Karma() {
        super("!karma");
        kc = new KarmaConnector();
    }

    /**
     * Messages the user's upVotes, downVotes, and upVotes - downVotes
     * in the channel.
     *
     * @param event the message event that triggered the command
     */
    @Override
    public void start(final MessageReceivedEvent event) {
        try {
            int upVotes = kc.getUserRow(event.getAuthor().getIdLong()).getInt("upvotes");
            int downVotes = kc.getUserRow(event.getAuthor().getIdLong()).getInt("downvotes");

            event.getChannel().sendMessage("Upvotes: " + upVotes
                    + "\nDownvotes: " + downVotes
                    + "\nKarma: " + (upVotes - downVotes)).queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

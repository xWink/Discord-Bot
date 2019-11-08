package command;

import database.KarmaConnector;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Karma extends Command {

    private KarmaConnector kc;

    /**
     * Initializes the command's key to "!karma".
     */
    Karma() {
        super("!karma");
        kc = new KarmaConnector();
    }


    /**
     * Checks if string matches to key.
     *
     * @param string the user's input being compared to the key
     * @return true if input equals key
     */
    @Override
    public boolean keyMatches(final String string) {
        return string.equalsIgnoreCase(getKey());
    }


    /**
     * Messages the user's upVotes, downVotes, and upVotes - downVotes
     * in the channel.
     *
     * @param event the message event that triggered the command
     */
    @Override
    public void start(final MessageReceivedEvent event) {
        long authorId = event.getAuthor().getIdLong();
        if (!kc.userExists(authorId)) {
            kc.addUser(authorId);
        }
        event.getChannel().sendMessage(getKarmaData(event)).queue();
    }

    private String getKarmaData(final MessageReceivedEvent event) {
        try {
            int upVotes = kc.getUserRow(event.getAuthor().getIdLong())
                    .getInt("upvotes");
            int downVotes = kc.getUserRow(event.getAuthor().getIdLong())
                    .getInt("downvotes");
            return "Upvotes: " + upVotes
                    + "\nDownvotes: " + downVotes
                    + "\nKarma: " + (upVotes - downVotes);
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to get karma data. Please contact a moderator!";
        }
    }
}

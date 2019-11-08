package command;

import database.KarmaConnector;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Karma extends Command {

    KarmaConnector kc;

    /**
     * Initializes the command's key to "!karma".
     */
    Karma() {
        super("!karma");
        kc = new KarmaConnector();
    }

    @Override
    public boolean keyMatches(String string) {
        return string.equalsIgnoreCase(getKey());
    }

    @Override
    public void start(MessageReceivedEvent event) {
        long authorId = event.getAuthor().getIdLong();
        if (!kc.userExists(authorId)) {
            kc.addUser(authorId);
        }
        event.getChannel().sendMessage(getKarmaData(event)).queue();
    }

    private String getKarmaData(MessageReceivedEvent event) {
        try {
            int upVotes = kc.getUserRow(event.getAuthor().getIdLong()).getInt("upvotes");
            int downVotes = kc.getUserRow(event.getAuthor().getIdLong()).getInt("downvotes");
            return "Upvotes: " + upVotes
                    + "\nDownvotes: " + downVotes
                    + "\nKarma: " + (upVotes - downVotes);
        } catch (Exception e) {
            e.printStackTrace();
            return"Failed to get karma data. Please contact a moderator!";
        }
    }
}

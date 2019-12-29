package command.commands.bang;

import command.Command;
import command.util.highscores.bang.BangHighScore;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class BangScores extends Command {

    /**
     * Initializes the command's key to "!bangscores".
     */
    public BangScores() {
        super("!bangscores", false);
    }

    /**
     * Prints the high scores for bang.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        event.getChannel().sendMessage(BangHighScore.getBangHighScore().toString()).queue();
    }
}

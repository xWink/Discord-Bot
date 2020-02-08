package command.commands.bang;

import command.Command;
import command.util.highscores.BangHighScore;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

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
        event.getChannel().sendMessage(BangHighScore.getBangHighScore().toEmbed().build()).queue();
    }
}

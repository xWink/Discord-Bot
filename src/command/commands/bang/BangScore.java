package command.commands.bang;

import command.Command;
import command.util.highscores.BangHighScore;
import main.Server;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class BangScore extends Command {

    /**
     * Initializes the command's key to "!bangscore".
     */
    public BangScore() {
        super("!bangscore", true);
    }

    /**
     * Prints the high scores for bang.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        if (event.getChannel().getIdLong() != 674369527731060749L
                && event.getChannel().getIdLong() != Server.getBotsChannel()) {
            return;
        }
        event.getChannel().sendMessage(BangHighScore.getBangHighScore().toString()).queue();
    }
}

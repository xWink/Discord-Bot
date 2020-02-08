package command.commands.bang;

import command.Command;
import command.util.highscores.BangHighScore;
import main.Server;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class BangScore extends Command {

    /**
     * Initializes the command's key to "!bangscore".
     */
    public BangScore() {
        super("!bangscore", true);
    }

    /**
     * Matches with either "!bangscore" or "!bangscores"
     * @param string the user's input being compared to the key
     * @return true if key matches "!bangscore" or "!bangscores"
     */
    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().matches("^" + getKey() + "s?$");
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

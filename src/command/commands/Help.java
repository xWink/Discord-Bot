package command.commands;

import command.Command;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Help extends Command {

    /**
     * Initializes the command's key to "!help".
     */
    public Help() {
        super("!help", true);
    }

    /**
     * Displays the help interface, which shows all the commands a regular
     * user has access to.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        event.getChannel().sendMessage("`!bang` - Play Russian roulette.\n\n"
                + "`!bangscores` - Shows high scores for bang.\n\n"
                + "`!bet`, `!hit`, `!stand` - Play blackjack against the computer.\n\n"
                + "`!daily` - Shows when your daily reward resets.\n\n"
                + "`!flip` - Flips a coin, displaying the result.\n\n"
                + "`!id [Discord ID]` - Shows a user's display name based on Discord ID.\n\n"
                + "`!info [course ID]` - Shows info on a course at UoGuelph.\n\n"
                + "`!join [role name]` - Apply to join or create a private elective channel.\n\n"
                + "`!karma` - Shows your upvotes, downvotes, and total karma.\n\n"
                + "`!leave [role name]` - Leave or unapply for a private elective channel.\n\n"
                + "`!market` and `!buy <item #>` - View and purchase items listed on the market.\n\n"
                + "`!mybang` - Shows your bang scores.\n\n"
                + "`!ping` - Shows your latency.\n\n"
                + "`!roles` - Shows a list of available roles to join.\n\n"
        ).queue();
    }
}

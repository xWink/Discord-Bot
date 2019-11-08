package command;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Help extends Command {

    /**
     * Initializes the command's key to "!help".
     */
    Help() {
        super("!help");
    }

    /**
     * Key matches if the string equals exactly (ignoring case) the key.
     *
     * @param string the user's input being compared to the key
     * @return true if the key matches the string
     */
    @Override
    public boolean keyMatches(String string) {
        return string.equalsIgnoreCase(getKey());
    }


    /**
     * Displays the help interface, which shows all the commands a regular
     * user has access to.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        event.getChannel().sendMessage(
                "`!join` & `!leave` - Join or leave a channel for your electives\n\n"
                        + "`!roles` - See available groups\n\n"
                        + "`!bang` - Play Russian Roulette\n\n"
                        + "`!bangscores` & `!mybang` - See bang high scores or personal stats\n\n"
                        + "`!bet`, `!hit`, `!hand`, & `!stand` - Play blackjack\n\n"
                        + "`!wallet` - Show how many GryphCoins you have\n\n"
                        + "`!market` & `!buy <item #>` - Use the GryphCoin market\n\n"
                        + "`karma` - Show how many upvotes and downvotes you have").queue();
    }
}

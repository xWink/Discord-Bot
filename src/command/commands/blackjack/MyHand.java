package command.commands.blackjack;

import command.Command;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class MyHand extends Command {

    /**
     * Initializes the command's key.
     */
    public MyHand() {
        super("!myhand", false);
    }

    /**
     * Shows the player's hand of cards.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        BlackJackGame game = BlackJackList.getUserGame(event.getAuthor().getIdLong());

        if (game == null) {
            event.getChannel().sendMessage("You haven't started a game yet!\n"
                    + "To start a new one, say `!bet <amount>`").queue();
            return;
        }

        event.getChannel().sendMessage(event.getAuthor().getName() + "'s hand is:\n"
                + game.getPlayer().getHand().toString()).queue();
    }
}

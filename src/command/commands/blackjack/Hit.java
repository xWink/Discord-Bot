package command.commands.blackjack;

import command.Command;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;


public class Hit extends Command {

    /**
     * Initializes the command's key to "!hit".
     */
    public Hit() {
        super("!hit", false);
    }

    /**
     * Every command must be able to be activated based on the event.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        String output = "";
        BlackJackGame game = BlackJackList.getUserGame(event.getAuthor().getIdLong());
        if (game == null) {
            event.getChannel().sendMessage("You haven't bet any GryphCoins yet!").queue();
            return;
        }

        try {
            int value = game.hit();
            if (value >= 21) {
                int reward = game.stand();
                output += value == 21 ? "You got 21!\n" : "You busted.\n";
                output += (reward >= 0 ? "You earned " : "You lost ") + reward + " *gc*";
                BlackJackList.removeGame(game);
            } else {
                output += "Your hand is now " + game.getPlayer().getHand().toString();
            }
            event.getChannel().sendMessage(output).queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

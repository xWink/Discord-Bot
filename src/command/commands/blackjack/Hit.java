package command.commands.blackjack;

import command.Command;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;


public class Hit extends Command {

    private EconomyConnector ec;

    /**
     * Initializes the command's key to "!hit".
     */
    public Hit() {
        super("!hit", false);
        ec = new EconomyConnector();
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
            event.getChannel().sendMessage("You haven't started a game yet!\n"
                    + "To start a new one, say `!bet <amount>`").queue();
            return;
        }

        try {
            int value = game.hit();
            output += event.getAuthor().getName() + "'s hand is now " + game.getPlayer().getHand().toString() + "\n";
            if (value >= 21) {
                int reward = game.checkWinner();
                output += value == 21 ? "You got 21!\n" : "You busted.\n";
                output += "Dealers hand: " + game.getDealer().getHand().toString() + "\n";
                if (game.getDealer().getHand().getValue() > 21) output += "Dealer busted!\n";
                if (value > 21 && game.getDealer().getHand().getValue() > 21) output += "Tie game, you didn't win or lose any money.";
                else output += (reward >= 0 ? "You earned " + reward : "You lost " + (0 - reward)) + " *gc*";
                ec.addOrRemoveMoney(event.getAuthor().getIdLong(), reward);
                BlackJackList.removeGame(game);
            }
            event.getChannel().sendMessage(output).queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

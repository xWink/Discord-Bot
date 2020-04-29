package command.commands.blackjack;

import command.Command;
import command.util.cards.CardMessage;
import command.util.cards.HandOfCards;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Stand extends Command {

    private EconomyConnector ec;

    /**
     * Initializes the command's key to "!stand".
     */
    public Stand() {
        super("!stand", false);
        ec = new EconomyConnector();
    }

    /**
     * Command where player stands in blackjack, ending the game and distributing
     * rewards or losses based on game results.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        BlackJackGame game = BlackJackList.getUserGame(event.getAuthor().getIdLong());
        String author = event.getAuthor().getName();

        if (game == null) {
            event.getChannel().sendMessage("You haven't started a game yet!\n"
                    + "To start a new one, say `!bet <amount>`").queue();
            return;
        }

        int reward = game.checkWinner();
        HandOfCards dealerHand = game.getDealer().getHand();
        String output = "Dealers hand: " + dealerHand.toString();

        // Show dealer hand
        CardMessage.createCardMessage(
                event.getChannel(),
                output,
                dealerHand.getAsList()
        ).queue();

        // Show winnings/losses/tie
        output = "";
        if (reward > 0)
            output += author + " wins! Earnings: " + reward + " *gc*";
        else if (reward < 0)
            output += author + " lost. Losses: " + (-reward) + " *gc*";
        else
            output += "Tie game, " + author + " didn't win or lose any money.";
        event.getChannel().sendMessage(output).queue();

        BlackJackList.removeGame(game);

        try {
            if (reward != 0) ec.addOrRemoveMoney(event.getAuthor().getIdLong(), reward);
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

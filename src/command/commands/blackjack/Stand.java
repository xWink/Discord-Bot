package command.commands.blackjack;

import command.Command;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

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
        String message;

        if (game == null) {
            event.getChannel().sendMessage("You haven't started a game yet!\n"
                    + "To start a new one, say `!bet <amount>`").queue();
            return;
        }

        int reward = game.checkWinner();
        message = "Dealers hand:\n" + game.getDealer().getHand().toString() + "\n";
        if (reward > 0) message += "You win! Earnings: " + reward + " *gc*";
        else if (reward < 0) message += "You lose. Losses: " + (0 - reward) + " *gc*";
        else message += "Tie game. You didn't win or lose any money";
        event.getChannel().sendMessage(message).queue();
        BlackJackList.removeGame(game);

        try {
            if (reward != 0) ec.addOrRemoveMoney(event.getAuthor().getIdLong(), reward);
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

package command.commands.blackjack;

import command.Command;
import command.util.cards.PhotoCombine;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

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
        String message = "";

        if (game == null) {
            event.getChannel().sendMessage("You haven't started a game yet!\n"
                    + "To start a new one, say `!bet <amount>`").queue();
            return;
        }

        int reward = game.checkWinner();

        try {
            File file = new File(".");
            String path = file.getAbsolutePath().replace("build/libs/.", "");
            InputStream stream = new FileInputStream(path + "res/out.png");

            if (PhotoCombine.genPhoto(game.getDealer().getHand().getHand())) {
                event.getChannel().sendMessage("Dealers hand:").addFile(stream, "out.png").queue();
            } else {
                event.getChannel().sendMessage("Dealers hand: " + game.getDealer().getHand().toString()).queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (reward > 0) message += author + " wins! Earnings: " + reward + " *gc*";
        else if (reward < 0) message += author + " lost. Losses: " + (-reward) + " *gc*";
        else message += "Tie game, " + author + " didn't win or lose any money.";
        event.getChannel().sendMessage(message).queue();
        BlackJackList.removeGame(game);

        try {
            if (reward != 0) ec.addOrRemoveMoney(event.getAuthor().getIdLong(), reward);
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

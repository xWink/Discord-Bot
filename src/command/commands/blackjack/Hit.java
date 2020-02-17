package command.commands.blackjack;

import command.Command;
import command.util.cards.HandOfCards;
import command.util.cards.PhotoCombine;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;
import java.io.FileInputStream;

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
        BlackJackGame game = BlackJackList.getUserGame(event.getAuthor().getIdLong());

        if (game == null) {
            event.getChannel().sendMessage("You haven't started a game yet!\n"
                    + "To start a new one, say `!bet <amount>`").queue();
            return;
        }

        try {
            File file = new File(".");
            String path = file.getAbsolutePath().replace("build/libs/.", "");

            String name = event.getAuthor().getName();
            String output = "";
            int playerValue = game.hit();

            if (PhotoCombine.genPhoto(game.getPlayer().getHand().getAsList())) {
                event.getChannel().sendMessage(name + "'s hand is now: " + game.getPlayer().getHand().toString())
                        .addFile(new FileInputStream(path + "res/out.png"), "out.png")
                        .queue();
            } else {
                event.getChannel().sendMessage(name + "'s hand is now: " + game.getPlayer().getHand().toString())
                        .queue();
            }

            if (playerValue >= 21) {
                int reward = game.checkWinner();
                HandOfCards dealerHand = game.getDealer().getHand();
                output += playerValue == 21 ? "You got 21!\n" : "You busted.\n";

                if (dealerHand.getValue() > 21) {
                    output += "Dealer busted!\n";
                }

                if (reward > 0) {
                    output += "You earned " + reward + " *gc*";
                } else if (reward < 0) {
                    output += "You lost " + (-reward) + " *gc*";
                } else {
                    output += "Tie game, you didn't win or lose any money";
                }
                output += "\nDealers hand: " + game.getDealer().getHand().toString();

                if (PhotoCombine.genPhoto(game.getDealer().getHand().getAsList())) {
                    event.getChannel().sendMessage(output)
                            .addFile(new FileInputStream(path + "res/out.png"), "out.png")
                            .queue();
                } else {
                    event.getChannel().sendMessage(output).queue();
                }

                if (reward != 0) {
                    ec.addOrRemoveMoney(event.getAuthor().getIdLong(), reward);
                }

                BlackJackList.removeGame(game);
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

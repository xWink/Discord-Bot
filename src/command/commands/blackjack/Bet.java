package command.commands.blackjack;

import command.Command;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import command.util.game.Player;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Bet extends Command {

    private EconomyConnector ec;

    /**
     * Initializes the command's key to "!bet".
     */
    public Bet() {
        super("!bet", false);
        ec = new EconomyConnector();
    }

    /**
     * Compares a string to the command's key and checks if that
     * string starts with the key.
     *
     * @param string the user's input being compared to the key
     * @return returns true if the key matches and false otherwise
     */
    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey());
    }

    /**
     * Takes a certain amount of money from the betting player and starts
     * a game of blackjack for them.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        if (!verifyInput(event)) return;
        long userId = event.getAuthor().getIdLong();
        MessageChannel channel = event.getChannel();
        String output = "";

        try {
            int betAmount = Integer.parseInt(event.getMessage().getContentRaw().split(" ")[1]);

            // Verify that the user has enough money
            if (!ec.canAfford(event.getAuthor().getIdLong(), betAmount)) {
                channel.sendMessage("You do not have enough money to make that bet!\n"
                        + "Your wallet contains " + ec.getWealth(userId) + " GryphCoins").queue();
                return;
            }

            BlackJackGame game = new BlackJackGame(new Player(userId), betAmount);
            game.start();
            output += event.getAuthor().getName() + " received their first 2 cards: "
                    + game.getPlayer().getHand().toString();

            // Check if started with blackjack
            if (game.getPlayer().getHand().getValue() == 21) {
                int result = game.checkWinner();
                output += "\nYou got 21!\nDealers hand: " + game.getDealer().getHand().toString() + "\n"
                        + (result > 0 ? "You won " + result + "*gc*!" : "It's a draw, you earned 0 *gc*");
                ec.addOrRemoveMoney(userId, result);
                game.end();
            } else {
                BlackJackList.addGame(game);
            }

            channel.sendMessage(output).queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    private boolean verifyInput(MessageReceivedEvent event) {
        long userId = event.getAuthor().getIdLong();
        MessageChannel channel = event.getChannel();

        // Verify that the user is not already in a game
        if (BlackJackList.getUserGame(userId) != null) {
            channel.sendMessage("You are already in a game\n"
                    + "To see your cards, say `!myhand`").queue();
            return false;
        }

        String[] strings = event.getMessage().getContentRaw().split(" ");
        int betAmount;

        try {
            betAmount = Integer.parseInt(strings[1]);
        } catch (Exception e) {
            channel.sendMessage("To bet for a game of blackjack, say `!bet <amount>`").queue();
            return false;
        }

        // Verify that user bet more than 0 GryphCoins
        if (betAmount < 1) {
            channel.sendMessage("You must bet at least 1 GryphCoin!").queue();
            return false;
        }

        return true;
    }
}

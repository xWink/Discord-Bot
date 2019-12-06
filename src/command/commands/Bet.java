package command.commands;

import command.Command;
import command.util.cards.DeckOfCards;
import database.connectors.BlackJackConnector;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Bet extends Command {

    private BlackJackConnector bjc;
    private EconomyConnector ec;

    /**
     * Initializes the command's key to "!bet".
     */
    public Bet() {
        super("!bet", false);
        bjc = new BlackJackConnector();
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
        long userId = event.getAuthor().getIdLong();
        MessageChannel channel = event.getChannel();
        if (!verifyInput(event)) return;

        int betAmount = Integer.parseInt(event.getMessage().getContentRaw().split(" ")[1]);

        // Verify that the user has enough money
        try {
            if (!ec.canAfford(event.getAuthor().getIdLong(), betAmount)) {
                channel.sendMessage("You do not have enough money to make that bet!\n"
                        + "Your wallet contains " + ec.getWealth(userId) + " GryphCoins").queue();
                return;
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
            return;
        }

        // Move betted money from player's wallet to the game's bet pool and start the game
        try {
            ec.addOrRemoveMoney(userId, 0 - betAmount);
            bjc.startGame(userId, betAmount, new DeckOfCards(1).pickRandomCard(), new DeckOfCards(1).pickRandomCard());
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    private boolean verifyInput(MessageReceivedEvent event) {
        long userId = event.getAuthor().getIdLong();
        MessageChannel channel = event.getChannel();

        // Verify that the user is not already in a game
        if (bjc.userExists(userId)) {
            channel.sendMessage("You cannot bet while already in a game!").queue();
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

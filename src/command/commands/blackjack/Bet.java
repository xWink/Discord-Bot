package command.commands.blackjack;

import command.Command;
import command.util.cards.Card;
import command.util.cards.HandOfCards;
import command.util.cards.PhotoCombine;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import command.util.game.Player;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.Collections;

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
        String name = event.getAuthor().getName();
        MessageChannel channel = event.getChannel();
        byte[] image;

        try {
            int betAmount = Integer.parseInt(event.getMessage().getContentRaw().split(" ")[1]);

            // Verify that the user has enough money
            if (!ec.canAfford(event.getAuthor().getIdLong(), betAmount)) {
                channel.sendMessage("You do not have enough money to make that bet!\n"
                        + "Your wallet contains " + ec.getWealth(userId) + " GryphCoins").queue();
                return;
            }

            // Start game
            BlackJackGame game = new BlackJackGame(new Player(userId), betAmount);
            game.start();
            Card dealersFirstCard = game.getDealer().getHand().getAsList().get(0);
            HandOfCards playerHand = game.getPlayer().getHand();

            // Show player's cards
            MessageAction message = channel.sendMessage(name + " received their first 2 cards: " + playerHand.toString());
            if ((image = PhotoCombine.genPhoto(playerHand.getAsList())) != null)
                message.addFile(image, "out.png").queue();
            message.queue();

            // Show dealer's first card
            message = channel.sendMessage("Dealer's first card: " + dealersFirstCard.toEmote());
            if ((image = PhotoCombine.genPhoto(Collections.singletonList(dealersFirstCard))) != null)
                message.addFile(image, "out.png");
            message.queue();

            // If game is not over, add it to list of active games
            if (playerHand.getValue() != 21) {
                BlackJackList.addGame(game);
                return;
            }

            // Check how much the player won or if game is a tie
            int result = game.checkWinner();
            String output;

            // Set output string
            if (result > 0)
                output = name + " got 21!\nYou won " + result + "*gc*!\n";
            else
                output = name + " got 21!\nIt's a draw, you earned 0 *gc*\n";
            output += "Dealers hand: " + game.getDealer().getHand().toString();

            // Send message with or without image
            message = channel.sendMessage(output);
            if ((image = PhotoCombine.genPhoto(game.getDealer().getHand().getAsList())) != null)
                message.addFile(image, "out.png");
            message.queue();

            // Update database and end game
            if (result > 0) ec.addOrRemoveMoney(userId, result);
            game.end();
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

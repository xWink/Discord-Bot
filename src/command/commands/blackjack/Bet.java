package command.commands.blackjack;

import command.Command;
import command.util.cards.Card;
import command.util.cards.CardMessage;
import command.util.cards.HandOfCards;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import command.util.game.Player;
import command.util.game.BlackJackStore;
import economy.EconomyService;
import economy.WagerLimits;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.Collections;

public class Bet extends Command {

    /** Largest blackjack stake allowed. */
    public static final int MAX_STAKE = 10;

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
     * Reserves the stake from the betting player and starts a game of
     * blackjack for them. Blackjack pays 3:2, ties push, a bust loses at once.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        if (!verifyInput(event)) return;
        long userId = event.getAuthor().getIdLong();
        String name = event.getAuthor().getName();
        MessageChannel channel = event.getChannel();

        try {
            int betAmount = Integer.parseInt(event.getMessage().getContentRaw().split(" ")[1]);

            String blocked = WagerLimits.check(userId, betAmount);
            if (blocked != null) {
                channel.sendMessage(blocked).queue();
                return;
            }
            channel.sendMessage("Stake: " + betAmount + " GC reserved. Maximum loss: " + betAmount
                    + " GC. Blackjack pays 3:2, a push returns your stake.").queue();

            // Start game. The stake is reserved and the hand saved in one transaction.
            BlackJackGame game = new BlackJackGame(new Player(userId), betAmount);
            game.setChannelId(channel.getIdLong());
            game.start();
            try {
                BlackJackStore.open(game);
            } catch (EconomyService.InsufficientFundsRuntime e) {
                channel.sendMessage("You do not have enough money to make that bet!\n"
                        + "Your spendable balance is " + ec.getSpendable(userId) + " GryphCoins").queue();
                return;
            } catch (SQLException e) {
                if (BlackJackList.getUserGame(userId) != null || isDuplicate(e)) {
                    channel.sendMessage("You are already in a game\nTo see your cards, say `!myhand`").queue();
                    return;
                }
                throw e;
            }

            Card dealersFirstCard = game.getDealer().getHand().getAsList().get(0);
            HandOfCards playerHand = game.getPlayer().getHand();

            // Show player's cards
            CardMessage.createCardMessage(
                    channel,
                    name + " received their first 2 cards: " + playerHand.toString(),
                    playerHand.getAsList()
            ).queue();

            // Show dealer's first card
            CardMessage.createCardMessage(
                    channel,
                    "Dealer's first card: " + dealersFirstCard.toEmote(),
                    Collections.singletonList(dealersFirstCard)
            ).queue();

            // If game is not over, add it to list of active games
            if (playerHand.getValue() != 21) {
                BlackJackList.addGame(game);
                return;
            }

            // Natural 21: settle immediately
            BlackJackList.addGame(game);
            int result = BlackJackStore.settle(game);
            String output;
            if (result > 0)
                output = name + " got 21!\nYou won " + result + "*gc*!\n";
            else
                output = name + " got 21!\nIt's a push, your stake is returned\n";
            output += "Dealers hand: " + game.getDealer().getHand().toString();

            CardMessage.createCardMessage(
                    channel,
                    output,
                    game.getDealer().getHand().getAsList()
            ).queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    private static boolean isDuplicate(SQLException e) {
        return e.getErrorCode() == 1062 || "23000".equals(e.getSQLState());
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

        int betAmount;
        String[] strings = event.getMessage().getContentRaw().split(" ");

        // Verify that user bet an integer
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

        // Stakes are capped at launch
        if (betAmount > MAX_STAKE) {
            channel.sendMessage("The maximum stake is " + MAX_STAKE + " GryphCoins.").queue();
            return false;
        }
        return true;
    }
}

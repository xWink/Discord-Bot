package DiscordBot.commands.blackjack;

import DiscordBot.util.cards.Hand;
import DiscordBot.util.economy.Wallet;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.sql.Connection;
import java.sql.ResultSet;

import static DiscordBot.commands.blackjack.BlackJack.*;

public class BlackJackCommands {

    public static void bet(User author, TextChannel channel, String content, Connection conn){

        // Verify that the user is not already in a game
        if (findGame(conn, author) != null){
            channel.sendMessage("You cannot bet while already in a game!").complete();
            return;
        }

        // Verify proper command format
        if (content.length() < 6 || !content.substring(5).matches("^[0-9]+$")) {
            channel.sendMessage("To bet for a game of blackjack, say `!bet <amount>`").complete();
            return;
        }

        // Verify that the user has enough money
        int betAmount = Integer.parseInt(content.substring(5));
        Wallet wallet = new Wallet(author, conn);

        if (betAmount > wallet.getWealth()){
            channel.sendMessage("You do not have enough money to make that bet!\n" +
                    "Your wallet contains " + wallet.getWealth() + " GryphCoins").complete();
            return;
        }

        // Move betted money from player's economy to the game's bet pool and start the game
        wallet.removeMoney(conn, betAmount);
        createNewGame(conn, author, channel, betAmount);
    }

    public static void hit(User author, TextChannel channel, Connection conn) {

        ResultSet rs;
        Hand hand;

        // If no game is active for the user
        if ((rs = findGame(conn, author)) == null) {
            channel.sendMessage("You haven't bet any GryphCoins yet!").complete();
            return;
        }

        // Add card
        addCard(author, conn, channel, rs);

        // Make sure hand is not null
        if ((hand = getPlayerHand(author, channel)) == null){
            channel.sendMessage("Error getting your hand").queue();
            return;
        }

        // Check if blackjack
        if (hand.getValue() == 21){
            channel.sendMessage("You got 21!").queue();
            int winner = checkWinner(channel, hand);
            endGame(author, conn, channel, winner);
        }

        // Check if busted
        else if (hand.getValue() > 21) {
            channel.sendMessage("You busted").queue();
            int winner = checkWinner(channel, hand);
            endGame(author, conn, channel, winner);
        }

        // Not 21 or busted, continue game as normal
        else
            channel.sendMessage(author.getName() + "'s hand is now:\n" + hand.showHand()).complete();
    }

    public static void myHand(User author, TextChannel channel){

        Hand hand;
        if ((hand = getPlayerHand(author, channel)) != null)
            channel.sendMessage(author.getName()+ "'s hand is:\n" + hand.showHand()).complete();
    }


    public static void stand(User author, TextChannel channel, Connection conn){

        Hand playerHand;
        int winner;

        // Check if a game exists for the user
        if (findGame(conn, author) == null){
            channel.sendMessage("You are not currently in a game!\n To start a new one, say `!hit`").queue();
            return;
        }

        // Make sure hand is not null
        if ((playerHand = getPlayerHand(author, channel)) == null){
            channel.sendMessage("Error getting your hand").queue();
            return;
        }

        // Verify winner
        winner = checkWinner(channel, playerHand);

        // Distribute reward and remove line from db
        endGame(author, conn, channel, winner);
    }
}

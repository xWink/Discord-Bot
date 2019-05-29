package DiscordBot.commands.blackjack;

import DiscordBot.util.cards.Hand;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.sql.Connection;
import java.sql.ResultSet;

import static DiscordBot.commands.blackjack.BlackJack.*;

public class BlackJackCommands {

    public static void hit(User author, MessageChannel channel) {

        Connection conn;
        ResultSet rs;
        Hand hand;

        // Connect to database
        if ((conn = connect()) == null) {
            System.out.println("Cannot connect to database, aborting hit command");
            channel.sendMessage("Can't connect to database. Please contact a moderator!").queue();
            return;
        }

        // If no game is active for the user
        if ((rs = findGame(conn, author)) == null) {
            createNewGame(conn, author, channel);
            return;
        }

        // Add card
        addCard(author, conn, channel, rs);

        // Make sure hand is not null
        if ((hand = getPlayerHand(author, channel)) == null){
            channel.sendMessage("Error getting your hand").queue();
            return;
        }

        // Check if over 21
        if (hand.getValue() > 21) {
            channel.sendMessage("You busted").queue();
            int winner = checkWinner(author, channel, hand);
            endGame(author, conn, channel, winner);
        } else
            channel.sendMessage(author.getName() + "'s hand is now:\n" + hand.showHand()).complete();
    }

    public static void myHand(User author, MessageChannel channel){

        Hand hand;
        if ((hand = getPlayerHand(author, channel)) != null)
            channel.sendMessage(author.getName()+ "'s hand is:\n" + hand.showHand()).complete();
    }


    public static void stand(User author, MessageChannel channel){

        Connection conn;
        Hand playerHand;
        int winner;

        // Connect to database
        if ((conn = connect()) == null){
            System.out.println("Cannot connect to database, aborting stand command");
            channel.sendMessage("Can't connect to database. Please contact a moderator!").queue();
            return;
        }

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
        winner = checkWinner(author, channel, playerHand);

        // Distribute reward and remove line from db
        endGame(author, conn, channel, winner);
    }
}

package DiscordBot.commands.tictactoe;

import DiscordBot.util.economy.Wallet;
import DiscordBot.util.tictactoe_util.Wager;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.sql.Connection;
import java.util.List;

public class TicTacToe {

    private static List<Wager> listOfWagers;

    private static int verifyInput(MessageChannel channel, String content, Message message){

        // If better number is too large, return -1
        if (!(content.substring(content.lastIndexOf(' ')).length() < 6))
            return -1;

        // Return 0 if matches format to challenge CPU
        if (content.matches("^ [0-9]*$")) {
            return 0;
        }

        // Return 1 if matches format to challenge human
        if (content.matches("^ @.* [0-9]*$")){
            if (message.getMentionedUsers().isEmpty()) {
                channel.sendMessage("Your target user does not exist!").complete();
                return -1;
            }
            return 1;
        }

        // Return -1 if fails verification
        return -1;
    }

    public static void wager(User author, MessageChannel channel, Connection conn, String content, Message message) throws InterruptedException {

        // Verify user input
        int verify = verifyInput(channel, content.substring(6), message);

        if (verify == -1){
            channel.sendMessage("Improper input").complete();
            return;
        }

        // Check if author already has a wager pending
        for (Wager w: listOfWagers) {
            if (w.getChallengerId() == author.getIdLong()){
                channel.sendMessage("You already have a pending wager!").complete();
                return;
            }
        }

        int wagerAmount;
        // Check if author can afford the wager
        Wallet challengerWallet = new Wallet(author, conn);
        if (!challengerWallet.canAfford(wagerAmount = Integer.parseInt(content.substring(content.lastIndexOf(' '))))){
            channel.sendMessage("You can't afford that wager!").complete();
            return;
        }

        Wager wager = null;

        // If challenging CPU, store wager in database
        if (verify == 0){

        }

        // If challenging human
        if (verify == 1) {
            // Check if target can afford the wager

            // Create wager instance
            wager = new Wager(author.getIdLong(), message.getMentionedUsers().get(0).getIdLong(), wagerAmount);
            listOfWagers.add(wager);
        }

        // Prune wager in 5 minutes
        int index = listOfWagers.size() - 1;
        Thread.sleep(300000);
        if (listOfWagers.size() > 0 && listOfWagers.get(index) != null && listOfWagers.get(index).equals(wager))
            listOfWagers.remove(index);
    }

    public static void accept(User author, MessageChannel channel, Connection conn, String content, Message message){

        // Verify input

        // Check if wager targeting author exists

        // Remove money from each player

        // Store wager in database

        // Remove wager from list
    }
}

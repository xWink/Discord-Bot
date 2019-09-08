package DiscordBot.commands.tictactoe;

import DiscordBot.util.tictactoe_util.Wager;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.sql.Connection;
import java.util.List;

public class TicTacToe {

    private static List<Wager> listOfWagers;

    private static int verifyInput(MessageChannel channel, String content){

        // Return 0 if matches format to challenge CPU

        // Return 1 if matches format to challenge human

        // Return -1 if fails verification
        return -1;
    }

    public static void wager(User author, MessageChannel channel, Connection conn, String content, Message message) throws InterruptedException {

        // Verify user input
        int verify = verifyInput(channel, content);

        // Check if author already has a wager in database

        // Check if author can afford the wager

        Wager wager = null;

        // If challenging CPU, store wager in database

        // If challenging human
        if (verify == 1) {
            // Check if target can afford the wager

            // Create wager instance
            wager = new Wager(author.getIdLong(), message.getMentionedUsers().get(0).getIdLong(), Integer.parseInt(content.substring(content.lastIndexOf(' '))));
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

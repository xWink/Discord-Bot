package DiscordBot.util.tictactoe_util;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.sql.Connection;
import java.util.ArrayList;

public class ListOfWagers {

    private static ArrayList<Wager> listOfWagers;

    private ListOfWagers(){

    }

    public static void addNewWager(Wager wager) {
        listOfWagers.add(wager);
    }

    public static void removeWager(Wager wager) {
        for (Wager w : listOfWagers) {
            if (w == wager) {
                listOfWagers.remove(w);
            }
        }
    }

    public static ArrayList<Wager> getWagers() {
        return listOfWagers;
    }



    public static void accept(User author, MessageChannel channel, Connection conn, String content, Message message){

        // Verify input

        // Check if wager targeting author exists

        // Remove money from each player

        // Store wager in database

        // Remove wager from list
    }
}

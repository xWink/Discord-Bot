package DiscordBot.commands.BlackJack;

import DiscordBot.RoleBot;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;

public class BlackJack {

    private static Connection connect(){

        // Connect to database
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            return DriverManager.getConnection("jdbc:mysql://localhost/discord_bot", RoleBot.config.db_user, RoleBot.config.db_pass);
        }
        catch (Exception e){
            System.out.println("BlackJack Exception 1\nException: "+ e.toString());
            return null;
        }
    }

    private static boolean gameInactive(Connection conn, User author){

        boolean inactive = true;

        try {
            PreparedStatement st = conn.prepareStatement("SELECT * FROM blackjack WHERE user=" + author.getIdLong());
            ResultSet rs = st.executeQuery();

            if (rs.next())
                inactive = false;
        }
        catch (Exception e){
            System.out.println("BlackJack Exception 2\nException: "+e.toString());
        }

        return inactive; // Return true if no active game is found, false if a game is found
    }

    private static void createNewGame(Connection conn, User author, MessageChannel channel){

        // Pick a random card as their first card
        ArrayList<Card> deck = Card.newDeck();
        System.out.println("Here0");
        Collections.shuffle(deck);
        System.out.println("Here1");
        String firstCard = deck.get(0).getRank().getSymbol() + deck.get(0).getSuit().getInitial();
        System.out.println("Here2");

        try {
            PreparedStatement st = conn.prepareStatement("INSERT INTO blackjack (user, card1) VALUES ("+author.getIdLong()+", "+firstCard+")");
            st.executeUpdate();
        }
        catch(Exception e){
            System.out.println("BlackJack Exception 3\nException: "+ e.toString());
            channel.sendMessage("Error, could not create a new game. Please contact a moderator!").queue();
        }

        channel.sendMessage("You received: "+deck.get(0).toEmote()).queue();
    }

    public static void hit(User author, MessageChannel channel){

        // Connect to database
        Connection conn;
        if ((conn = connect()) == null){
            System.out.println("Cannot connect to database, aborting hit command");
            channel.sendMessage("Can't connect to database. Please contact a moderator!").queue();
            return;
        }

        // If no game is active for the user
        if (gameInactive(conn, author)){
            createNewGame(conn, author, channel);
            return;
        }

        //Add card

        //Check if over 21

        //If over 21, take their money and remove their line from the db

        //If under 21, show hand and update db
    }

    public static void stand(User author, MessageChannel channel){

        // Connect to database
        Connection conn;
        if ((conn = connect()) == null){
            System.out.println("Cannot connect to database, aborting stand command");
            channel.sendMessage("Can't connect to database. Please contact a moderator!").queue();
            return;
        }

        // Check if a game exists for the user
        if (!gameInactive(conn, author)){
            channel.sendMessage("You are not currently in a game!\n To start a new one, say `!hit`").queue();
            return;
        }

        // Get dealer's hand

        // Decide winner

        // Distribute reward and remove line from db
    }
}

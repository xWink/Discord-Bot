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

import static DiscordBot.commands.BlackJack.CardRank.TEN;
import static DiscordBot.commands.BlackJack.CardSuit.SPADES;

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

    private static ResultSet findGame(Connection conn, User author){

        try {
            PreparedStatement st = conn.prepareStatement("SELECT * FROM blackjack WHERE user=" + author.getIdLong());
            ResultSet rs = st.executeQuery();

            if (rs.next())
                return rs; // Return the result set if game found
        }
        catch (Exception e){
            System.out.println("BlackJack Exception 2\nException: "+e.toString());
        }

        return null; // Return null if no game found
    }

    private static void createNewGame(Connection conn, User author, MessageChannel channel){

        Card firstCard = pickRandomCard(channel);
        String cardString = firstCard.toDbFormat();

        try {
            PreparedStatement st = conn.prepareStatement("INSERT INTO blackjack (user, card1) VALUES ("+author.getIdLong()+", '"+cardString+"')");
            st.executeUpdate();
        }
        catch(Exception e){
            System.out.println("BlackJack Exception 3\nException: "+ e.toString());
            channel.sendMessage("Error, could not create a new game. Please contact a moderator!").queue();
        }
    }

    private static Card pickRandomCard(MessageChannel channel){

        ArrayList<Card> deck = Card.newDeck(); // Create deck of cards
        Collections.shuffle(deck); // Shuffle the deck
        Card card = deck.get(0); // Pick the top card from the deck

        channel.sendMessage("You received: "+card.toEmote()).queue();

        return card;
    }

    private static void addCard(User author, Connection conn, MessageChannel channel, ResultSet rs){

        Card newCard = pickRandomCard(channel);

        try {
            // Find first empty column
            int index = 1;
            while (rs.getString("card"+index) != null)
                index++;

            PreparedStatement st = conn.prepareStatement("UPDATE blackjack SET card"+index+" = '"+newCard.toDbFormat()+"' WHERE user = "+author.getIdLong());
            st.executeUpdate();
        }
        catch(Exception e){
            System.out.println("BlackJack Exception 4\nException: "+ e.toString());
            channel.sendMessage("Error, could not add card to database. Please contact a moderator!").queue();
        }
    }

    private static Hand getHand(User author, MessageChannel channel){

        Connection conn;
        ResultSet rs;
        int i = 1;
        String cardString;
        Hand hand = new Hand();

        // Connect to database
        if ((conn = connect()) == null){
            System.out.println("Cannot connect to database, aborting stand command");
            channel.sendMessage("Can't connect to database. Please contact a moderator!").queue();
            return null;
        }

        // Check if a game exists for the user
        if ((rs = findGame(conn, author)) == null){
            channel.sendMessage("You are not currently in a game!\n To start a new one, say `!hit`").queue();
            return null;
        }

        try {
            // For each card in the player's hand
            while ((cardString = rs.getString("card" + i)) != null){
                String rankString;
                String suitString;

                // If the rank is anything other than 10
                if (cardString.length() == 3) {
                    rankString = cardString.substring(0, 2);
                    suitString = cardString.substring(2);
                }
                else{ // rank is 10
                    rankString = cardString.substring(0, 3);
                    suitString = cardString.substring(3);
                }

                CardRank rank = CardRank.getBySymbol(rankString); // Make rank object
                CardSuit suit = CardSuit.getByInitial(suitString); // Make suit object

                Card card = new Card(rank, suit); // Make card object
                hand.add(card);

                i++;
            }
        }
        catch (Exception e){
            System.out.println("BlackJack Exception 5\nException: "+ e.toString());
            channel.sendMessage("Error, could not get hand. Please contact a moderator!").queue();
        }

        return hand;
    }

    private static void endGame(User author, Connection conn, MessageChannel channel){

        // Delete user's line from database
        try {
            PreparedStatement st = conn.prepareStatement("DELETE FROM blackjack WHERE user = " + author.getIdLong());
            st.executeUpdate();
        }
        catch(Exception e){
            System.out.println("BlackJack Exception 6\nException: "+ e.toString());
            channel.sendMessage("Error, could not end your game. Please contact a moderator!").queue();
        }
    }

    public static void hit(User author, MessageChannel channel){

        Connection conn;
        ResultSet rs;

        // Connect to database
        if ((conn = connect()) == null){
            System.out.println("Cannot connect to database, aborting hit command");
            channel.sendMessage("Can't connect to database. Please contact a moderator!").queue();
            return;
        }

        // If no game is active for the user
        if ((rs = findGame(conn, author)) == null){
            createNewGame(conn, author, channel);
            return;
        }

        //Add card
        addCard(author, conn, channel, rs);

        //Check if over 21

        //If over 21, take their money and remove their line from the db

        //If under 21, show hand and update db
    }

    public static void myHand(User author, MessageChannel channel){

        Hand hand;
        if ((hand = getHand(author, channel)) != null)
            hand.showHand();
    }


    public static void stand(User author, MessageChannel channel){

        Connection conn;
        ResultSet rs;

        // Connect to database
        if ((conn = connect()) == null){
            System.out.println("Cannot connect to database, aborting stand command");
            channel.sendMessage("Can't connect to database. Please contact a moderator!").queue();
            return;
        }

        // Check if a game exists for the user
        if ((rs = findGame(conn, author)) == null){
            channel.sendMessage("You are not currently in a game!\n To start a new one, say `!hit`").queue();
            return;
        }

        // Get dealer's hand

        // Decide winner

        // Distribute reward and remove line from db
        endGame(author, conn, channel);

        channel.sendMessage("Ended your game!").queue();
    }
}

package DiscordBot.commands.BlackJack;

import DiscordBot.Cards.Card;
import DiscordBot.Cards.CardRank;
import DiscordBot.Cards.CardSuit;
import DiscordBot.Cards.Hand;
import DiscordBot.RoleBot;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

class BlackJack {

    static Connection connect(){

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

    static ResultSet findGame(Connection conn, User author){

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

    static void createNewGame(Connection conn, User author, MessageChannel channel){

        Card firstCard = Card.pickRandomCard();
        Card secondCard = Card.pickRandomCard();
        String firstCardString = firstCard.toDbFormat();
        String secondCardString = secondCard.toDbFormat();

        try {
            PreparedStatement st = conn.prepareStatement("INSERT INTO blackjack (user, card1, card2) VALUES ("+author.getIdLong()+", '"+firstCardString+"', '"+secondCardString+"')");
            st.executeUpdate();
        }
        catch(Exception e){
            System.out.println("BlackJack Exception 3\nException: "+ e.toString());
            channel.sendMessage("Error, could not create a new game. Please contact a moderator!").queue();
        }

        channel.sendMessage(author.getName() + " received their first 2 cards: " + firstCard.toEmote() + " " + secondCard.toEmote()).complete();
    }

    static void addCard(User author, Connection conn, MessageChannel channel, ResultSet rs){

        Card newCard = Card.pickRandomCard();
        channel.sendMessage(author.getName() + " received: " + newCard.toEmote()).complete();

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

    static Hand getPlayerHand(User author, MessageChannel channel){

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

    private static Hand getDealerHand(){

        Hand dealerHand = new Hand();

        // Give dealer new cards until hand value >= 18
        while (dealerHand.getValue() < 18)
            dealerHand.add(Card.pickRandomCard());

        return dealerHand;
    }

    static int checkWinner(User author, MessageChannel channel, Hand playerHand){

        Hand dealerHand;

        // Get dealer's hand
        dealerHand = getDealerHand();
        channel.sendMessage("The dealer's hand is:\n" + dealerHand.showHand()).complete();

        // Make sure hand is not null
        if (playerHand == null){
            channel.sendMessage("Error getting your hand").queue();
            return -2;
        }

        // If dealer wins
        if (dealerHand.getValue() <= 21 && (playerHand.getValue() < dealerHand.getValue() || playerHand.getValue() > 21)){
            channel.sendMessage("Dealer wins :(").queue();
            return -1;
        }
        // If player wins
        else if (playerHand.getValue() <= 21 && (dealerHand.getValue() < playerHand.getValue() || dealerHand.getValue() > 21)){
            channel.sendMessage(author.getName() + " wins! :money_mouth:").queue();
            return 1;
        }
        // If tied
        else{
            channel.sendMessage("Tie game :upside_down:").queue();
            return 0;
        }
    }

    static void endGame(User author, Connection conn, MessageChannel channel, int winner){

        switch (winner){
            case -1:
                System.out.println("Dealer wins");
                break;
            case 0:
                System.out.println("Tie game");
                break;
            case 1:
                System.out.println("Player wins");
                break;
        }

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
}

package DiscordBot.util.economy;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

public class Market {

    private ArrayList<Listing> listings = new ArrayList<>();

    public Market(Guild guild){

        // Add any listings to market here
        listings.add(new Listing(guild.getRolesByName("orange", true).get(0), 75));
        listings.add(new Listing(guild.getRolesByName("blue", true).get(0), 100));
        listings.add(new Listing(guild.getRolesByName("green", true).get(0), 150));
        listings.add(new Listing(guild.getRolesByName("purple", true).get(0), 200));
        listings.add(new Listing(guild.getRolesByName("pink", true).get(0), 300));
    }

    public ArrayList<Listing> getListings(){
        return listings;
    }

    public void showListings(TextChannel channel){

        // Make string that contains a list of all colour roles available
        String string = "Colours available:\n";
        int i = 0;

        for (Listing colour : listings){
            i++;
            string = string.concat(i + ". " + colour.getRole().getName() + " - " + colour.getCost() + " *gc*/week\n");
        }

        channel.sendMessage(string).complete();
    }

    public void purchase(User user, Connection conn, String content, TextChannel channel) throws SQLException{

        Wallet userWallet = new Wallet(user, conn);

        // Check for improper input
        if (content.length() < 11 ||
                !content.substring(10).matches("^[0-9]+$") ||
                Integer.parseInt(content.substring(10)) < 1 ||
                Integer.parseInt(content.substring(10)) > this.listings.size()){
            channel.sendMessage("To purchase a colour, say `!purchase <colour number>`").complete();
        }

        // Check if user already bought a role
        else if (hasColour(user, conn))
            channel.sendMessage("You already have a colour!").complete();

        // Check if user can afford the role
        else if (!userWallet.canAfford(listings.get(Integer.parseInt(content.substring(10)) - 1).getCost())){
            channel.sendMessage("You can't afford to buy this role. Your wallet contains " +
                    userWallet.getWealth() + " GryphCoins").complete();
        }

        // Give user the role and set expiry time
        else{
            giveColour(user, conn, channel, Integer.parseInt(content.substring(10)) - 1);
        }
    }

    private boolean hasColour(User user, Connection conn) throws SQLException {

        ResultSet rs = conn.prepareStatement("SELECT * FROM economy WHERE user = "+user.getIdLong()).executeQuery();
        return rs.next() && rs.getLong("role_expiry") > 0;
    }

    private void giveColour(User user, Connection conn, TextChannel channel, int index) throws SQLException {

        Date date = new Date();
        // Assign role in server
        channel.getGuild().getController().addSingleRoleToMember(channel.getGuild().getMember(user),
                listings.get(index).getRole()).complete();

        // Set expiry time in database
        conn.prepareStatement("UPDATE economy SET role_expiry = " + (date.getTime() + 604800000)).executeUpdate();
    }
}
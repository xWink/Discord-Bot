package DiscordBot.util.economy;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.sql.Connection;
import java.util.ArrayList;

public class Market {

    private ArrayList<Listing> listings;

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

        String string = "Colours available for sale:\n";
        int i = 0;

        for (Listing colour : listings){
            i++;
            string = string.concat(i + ". " + colour.getRole().getName() + " - " + colour.getCost() + "\n");
        }

        channel.sendMessage(string).complete();
    }

    public void purchase(User user, Connection conn, String content, TextChannel channel){

        Wallet userWallet = new Wallet(user, conn);

        if (content.length() < 11 || !content.substring(10).matches("^[0-9]+$")){
            channel.sendMessage("To purchase a colour, say `!purchase <colour number>`").complete();
        }
        else if (!userWallet.canAfford(listings.get(Integer.parseInt(content.substring(10))).getCost())){
            channel.sendMessage("You can't afford to buy this role. Your wallet contains " +
                    userWallet.getWealth() + "GryphCoins").complete();
        }
        else{
            channel.sendMessage("This is a test, purchase not completed.\nTest successful").complete();
        }
    }
}
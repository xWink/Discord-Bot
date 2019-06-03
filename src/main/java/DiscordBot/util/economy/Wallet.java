package DiscordBot.util.economy;

import net.dv8tion.jda.core.entities.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Wallet {

    private User user;
    private int wealth;

    public Wallet(User user, Connection conn){
        this.user = user;

        ResultSet rs;
        try {
            if ((rs = findUserWallet(user, conn)) != null) {
                if (!rs.next()) { // Check if user is in the economy
                    addUserToEconomy(user, conn); // Add new users
                    this.wealth = 5; // Default wealth
                }
                else {
                    this.wealth = rs.getInt("wallet"); // Get user's wealth
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public User getWalletUser(){
        return user;
    }

    public int getWealth(){
        return wealth;
    }

    private static ResultSet findUserWallet(User user, Connection conn){

        try {
            PreparedStatement st = conn.prepareStatement("SELECT * FROM economy WHERE user = " + user.getIdLong());
            return st.executeQuery();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    private static int addUserToEconomy(User user, Connection conn){

        try {
            conn.prepareStatement("INSERT INTO economy (user, wallet) VALUES (" + user.getIdLong() + ", 5)").executeUpdate();
            return 1;
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    public int addMoney(Connection conn, int amount){

        try {
            conn.prepareStatement("UPDATE economy SET wallet = wallet + " + amount + " WHERE user = " + this.user.getIdLong()).executeQuery();
            return 1;
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    public int removeMoney(Connection conn, int amount){

        try {
            conn.prepareStatement("UPDATE economy SET wallet = wallet - " + amount + " WHERE user = " + this.user.getIdLong()).executeQuery();
            return 1;
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    public boolean canAfford(int cost){

        return this.wealth - cost >= 0;
    }
}

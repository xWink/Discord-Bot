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

    private static ResultSet findUserWallet(User user, Connection conn) throws SQLException{
        PreparedStatement st = conn.prepareStatement("SELECT * FROM economy WHERE user = " + user.getIdLong());
        return st.executeQuery();
    }

    private static void addUserToEconomy(User user, Connection conn) throws SQLException{
        conn.prepareStatement("INSERT INTO economy (user, wallet, role_expiry, role_colour) VALUES (" + user.getIdLong() + ", 5, 0, NULL)").executeUpdate();
    }

    public void addMoney(Connection conn, int amount) throws SQLException{
        conn.prepareStatement("UPDATE economy SET wallet = wallet + " + amount + " WHERE user = " + this.user.getIdLong()).executeQuery();
    }

    public void removeMoney(Connection conn, int amount) throws SQLException{
        conn.prepareStatement("UPDATE economy SET wallet = wallet - " + amount + " WHERE user = " + this.user.getIdLong()).executeQuery();
    }

    public boolean canAfford(int cost){

        return this.wealth - cost >= 0;
    }
}

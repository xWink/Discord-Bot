package database.connectors;

import database.Connector;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public final class JackPotConnector extends Connector {

    /**
     * @see Connector
     * Initializes the table as "jackpot".
     */
    public JackPotConnector() {
        super("jackpot");
    }


    /**
     * Searches for any player who has already asked to join Lottery
     *
     * @param userId the id number of the Discord user being searched for
     * @return true if found, false if not found or error occurs
     */
    public boolean userInLotto(long userId) {
        return !(super.userExists(userId, getTable()));
    }
    /**
    * clears the jackpot table
    *
    */
    public void clearTable (){
        try {
            super.customUpdate("DELETE FROM jackpot");
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    /*
    *HEY WINK DUNNO HOW TO SET UP THE SQL DATA BASE
    * so if it is set up I would need in a row the following
    *
    * user, lottoId
    * the user is the id 
    * lotto is the lotto number for when the ticket is drawn 
    */

    /**
     *
     * @param lottoId the lotto id
     * @return the user id of the winner
     */
    public long getWinnerID (int lottoId){
        try {
            ResultSet resultSet = getConnection().prepareStatement("SELECT * FROM jackpot "
                    + "WHERE lotto= " + lottoId).executeQuery();
            return resultSet.getLong("user");
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
    public String getWinnerName (int lottoId){
        try {
            ResultSet resultSet = getConnection().prepareStatement("SELECT * FROM jackpot "
                    + "WHERE lotto= " + lottoId).executeQuery();
            return resultSet.getString("name");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    /**
     * Adds a new user to the Lottery based off the Lotto Number and ID.
     *
     * @param userId the ID number of the new user being added
     */
    public void addLotto(long userId, int lottoId, String name) {
        try {
            PreparedStatement stmt = getConnection().prepareStatement("INSERT INTO " + getTable()
                    + " (user, lotto, name) "
                    + "VALUES (" + userId + ", " + lottoId + ", ?)");
                    stmt.setString(1, name);
                    stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addUser(long userId ) {}
}

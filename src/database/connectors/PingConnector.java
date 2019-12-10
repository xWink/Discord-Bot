package database.connectors;

import database.Connector;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class PingConnector extends Connector {

    /**
     * Initializes table to "ping".
     */
    public PingConnector() {
        super("ping");
    }


    /**
     * Searches the ping table for a row with a matching user ID and returns
     * whether or not such a row was found.
     *
     * @param userId the id number of the Discord user being searched for
     * @return true if found, false if not found or error occurs
     */
    private boolean userExists(long userId) {
        return super.userExists(userId, getTable());
    }


    /**
     * Checks if ping value is the new max.
     *
     * @param userId the Discord user's ID number
     * @param ping the ping value the user just achieved
     * @return true if the ping value is higher than max, otherwise false
     * @throws SQLException may be thrown when getting "max" row in a ResultSet
     */
    public boolean isMax(long userId, int ping) throws SQLException {
        if (!userExists(userId)) addUser(userId);
        ResultSet rs = getUserRow(userId, getTable());
        return ping > rs.getInt("max");
    }


    /**
     * Checks if ping value is the new min.
     *
     * @param userId the Discord user's ID number
     * @param ping the ping value the user just achieved
     * @return true if the ping value is lower than min, otherwise false
     * @throws SQLException may be thrown when getting "min" row in a ResultSet
     */
    public boolean isMin(long userId, int ping) throws SQLException {
        if (!userExists(userId)) addUser(userId);
        ResultSet rs = getUserRow(userId, getTable());
        return ping < rs.getInt("min");
    }


    /**
     * Updates the max value of a user's row in the table.
     *
     * @param userId the ID of the user whose row is being updated
     * @param ping the new max ping for the user
     */
    public void setMaxPing(long userId, int ping) {
        if (!userExists(userId)) addUser(userId);
        try {
            getConnection().prepareStatement("UPDATE " + getTable()
                    + " SET max = " + ping
                    + " WHERE user = " + userId).executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Updates the min value of a user's row in the table.
     *
     * @param userId the ID of the user whose row is being updated
     * @param ping the new min ping for the user
     */
    public void setMinPing(long userId, int ping) {
        if (!userExists(userId)) addUser(userId);
        try {
            getConnection().prepareStatement("UPDATE " + getTable()
                    + " SET min = " + ping
                    + " WHERE user = " + userId).executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Searches the ping table for a row with matching user ID and returns
     * the ResultSet of the query.
     *
     * @param userId the id number of the Discord user being searched for
     * @return the ResultSet of the query
     */
    public ResultSet getUserRow(long userId) {
        if (!userExists(userId)) addUser(userId);
        ResultSet rs = getUserRow(userId, getTable());
//        try {
//            rs.next();
//        } catch (SQLException ignored) { }
        return rs;
    }


    /**
     * Adds a new user to the ping table based on their ID.
     *
     * @param userId the ID number of the new user being added
     */
    @Override
    public void addUser(long userId) {
        try {
            getConnection().prepareStatement("INSERT INTO " + getTable() + " VALUES('" + userId + "', 0, 0)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

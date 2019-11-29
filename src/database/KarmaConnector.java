package database;

import java.sql.Connection;
import java.sql.ResultSet;

public class KarmaConnector extends Connector {

    /**
     * Initializes the table as "karma".
     */
    public KarmaConnector() {
        super("karma");
    }


    /**
     * Adds/removes upVotes to/from the user in the database.
     *
     * @param userId the user being affected
     * @param numUpVotes the number of upVotes to add (pass negative value to remove votes)
     */
    public void updateUpVotes(long userId, int numUpVotes) {
        if (!userExists(userId)) addUser(userId);
        try {
            getConnection().prepareStatement("UPDATE " + getTable()
                    + " SET upvotes = upvotes + " + numUpVotes
                    + " WHERE user = " + userId).executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Adds/removes downVotes to/from the user in the database.
     *
     * @param userId the user being affected
     * @param numDownVotes the number of downVotes to add (pass negative value to remove votes)
     */
    public void updateDownVotes(long userId, int numDownVotes) {
        if (!userExists(userId)) addUser(userId);
        try {
            getConnection().prepareStatement("UPDATE " + getTable()
                    + " SET downvotes = downvotes + " + numDownVotes
                    + " WHERE user = " + userId).executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Searches the karma table for a row with a matching user ID and returns
     * whether or not such a row was found.
     *
     * @param userId the id number of the Discord user being searched for
     * @return true if found, false if not found or error occurs
     */
    private boolean userExists(long userId) {
        return super.userExists(userId, getTable());
    }


    /**
     * Searches the karma table for a row with matching user ID and returns
     * the ResultSet of the query.
     *
     * @param userId the id number of the Discord user being searched for
     * @return the ResultSet of the query
     */
    public ResultSet getUserRow(long userId) {
        if (!userExists(userId)) addUser(userId);
        return super.getUserRow(userId, getTable());
    }


    /**
     * Connection getter.
     *
     * @return the database connection
     */
    @Override
    public Connection getConnection() {
        return super.getConnection();
    }


    /**
     * Adds a new user to the karma table based on their ID.
     *
     * @param userId the ID number of the new user being added
     */
    @Override
    public void addUser(long userId) {
        try {
            getConnection().prepareStatement("INSERT INTO " + getTable()
                    + " (user, upvotes, downvotes) "
                    + "VALUES (" + userId + ", 0, 0)").executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

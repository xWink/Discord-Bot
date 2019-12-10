package database.connectors;

import database.Connector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public final class BangConnector extends Connector {

    /**
     * Initializes the table as "bang".
     */
    public BangConnector() {
        super("bang");
    }

    /**
     * Searches the bang table for a row with a matching user ID and returns
     * whether or not such a row was found.
     *
     * @param userId the id number of the Discord user being searched for
     * @return true if found, false if not found or error occurs
     */
    private boolean userExists(long userId) {
        return super.userExists(userId, getTable());
    }

    /**
     * Checks if the user is eligible for their daily reward.
     *
     * @param userId the Discord user's ID number
     * @return true if the user if eligible, false if not
     * @throws SQLException may be thrown when accessing the long "last_daily" from a ResultSet
     */
    public boolean isEligibleForDaily(long userId) throws SQLException {
        return new Date().getTime() - getUserRow(userId, getTable())
                .getLong("last_daily") >= 86400000;
    }

    /**
     * Updates a user's row in the table based on the results of a bang.
     *
     * @param userId the user's ID number
     * @param jammed whether the gun jammed
     * @param killed whether the user was killed (gun went bang)
     * @param reward whether the user received their daily reward
     * @throws SQLException may be thrown when making a prepared statement
     */
    public void updateUserRow(long userId, boolean jammed, boolean killed, boolean reward) throws SQLException {
        if (!userExists(userId)) addUser(userId);
        getConnection().prepareStatement("UPDATE bang SET tries = tries + 1,"
                + " last_played = " + new Date().getTime()
                + (jammed ? ", jams = jams + 1" : "")
                + (killed ? ", deaths = deaths + 1" : "")
                + (reward ? ", last_daily = last_played" : "")
                + " WHERE user = " + userId).executeUpdate();
    }

    /**
     * Returns a user's row in the table.
     *
     * @param userId the user's ID number
     * @return a ResultSet containing the user's entire row in bang
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
     * Returns the time at which a user last received a daily reward.
     *
     * @param userId the user's ID number
     * @return the long int that is the time at which the last daily reward was received
     * @throws SQLException may be thrown when making a prepared statement
     */
    public long getDaily(long userId) throws SQLException {
        if (!userExists(userId)) addUser(userId);
        ResultSet rs = getConnection()
                .prepareStatement("SELECT last_daily FROM bang WHERE user=" + userId)
                .executeQuery();
        rs.next();
        return rs.getLong("last_daily") + 86460000;
    }

    /**
     * Adds a new user to the bang table based on their ID.
     *
     * @param userId the ID number of the new user being added
     */
    @Override
    public void addUser(long userId) {
        try {
            getConnection().prepareStatement("INSERT INTO bang "
                    + "(user, tries, deaths, jams, last_played) "
                    + "VALUES (" + userId + ", 0, 0, 0, 0").executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

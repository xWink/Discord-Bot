package database.connectors;

import command.util.highscores.bang.BangPlayer;
import database.Connector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

public final class BangConnector extends Connector {

    /**
     * @see Connector
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
     * Sets the new last daily reward time for a specified user.
     *
     * @param userId the user's ID number
     * @param lastPlayedTime the time in ms since epoch that is the new user's daily
     * @throws SQLException may be thrown when making a prepared statement
     */
    public void setDaily(long userId, long lastPlayedTime) throws SQLException {
        if (!userExists(userId)) addUser(userId);
        getConnection().prepareStatement("UPDATE bang SET last_daily = " + lastPlayedTime
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
        return getUserRow(userId, getTable());
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
     * Gets a list of the Bang players with the most attempts in order of
     * greatest to least.
     *
     * @return list of top 10 bang attempts players
     * @throws SQLException may be thrown when interacting with database
     */
    public ArrayList<BangPlayer> getMostAttemptsPlayers() throws SQLException {
        ResultSet resultSet = getConnection().prepareStatement("SELECT user, tries FROM bang "
                + "WHERE " + new Date().getTime() + " - last_played < 604800000 "
                + "GROUP BY user, tries "
                + "ORDER BY tries").executeQuery();

        ArrayList<BangPlayer> players = new ArrayList<>();
        if (resultSet.last()) {
            do {
                players.add(new BangPlayer(resultSet.getLong("user"),
                        resultSet.getInt("tries"), resultSet.getInt("deaths"),
                        resultSet.getInt("jams")));
            } while (resultSet.previous());
        }
        return players;
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

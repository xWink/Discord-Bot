package database.connectors;

import database.Connector;
import command.util.economy.RoleListing;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public final class EconomyConnector extends Connector {

    /**
     * Initializes the table to "economy".
     */
    public EconomyConnector() {
        super("economy");
    }

    /**
     * Adds/removes a specified amount of money to the user's wealth column in
     * the economy table.
     *
     * @param userId the ID number of the user getting/losing money
     * @param amount the amount of money to add (negative value to remove)
     * @throws SQLException may be thrown when creating a PreparedStatement
     */
    public void addOrRemoveMoney(long userId, int amount) throws SQLException {
        if (!userExists(userId)) addUser(userId);
        getConnection().prepareStatement("UPDATE " + getTable()
                + " SET wallet = wallet + " + amount
                + " WHERE user = " + userId).executeUpdate();
    }

    /**
     * Checks if the user can afford a certain expense.
     *
     * @param userId the ID number of the user
     * @param cost the cost that is being compared to the user's wealth
     * @return true if the user can afford the expense, false otherwise
     * @throws SQLException may be thrown by getting "wallet" integer in ResultSet
     */
    public boolean canAfford(long userId, int cost) throws SQLException {
        if (!userExists(userId)) addUser(userId);
        return getUserRow(userId).getInt("wallet") - cost >= 0;
    }

    /**
     * Getter for the amount of wealth a user has.
     *
     * @param userId the ID number of the user
     * @return the amount of money the user has
     * @throws SQLException may be thrown by getting "wallet" integer in ResultSet
     */
    public int getWealth(long userId) throws SQLException {
        if (!userExists(userId)) addUser(userId);
        return getUserRow(userId).getInt("wallet");
    }

    /**
     * Checks if a user already has an active colour role.
     *
     * @param userId the ID number of the user
     * @return true if the user has a colour role, false if not
     * @throws SQLException may be thrown when creating a PreparedStatement
     */
    public boolean userHasColour(long userId) throws SQLException {
        if (!userExists(userId)) addUser(userId);
        ResultSet rs = getConnection()
                .prepareStatement("SELECT * FROM economy WHERE user = " + userId)
                .executeQuery();
        rs.next();
        return rs.getLong("role_expiry") > 0;
    }

    /**
     * Sets a user's role and its expiry time in the database.
     *
     * @param userId the ID number of the user
     * @param listing the RoleListing that the user purchased
     * @throws SQLException may be thrown when creating a PreparedStatement
     */
    public void setRole(long userId, RoleListing listing) throws SQLException {
        if (!userExists(userId)) addUser(userId);
        Date date = new Date();
        getConnection().prepareStatement("UPDATE economy "
                + "SET role_expiry = " + (date.getTime() + listing.getDuration() * 86400000)
                + ", role_colour = '" + listing.getRole().getName()
                + "' WHERE user = " + userId).executeUpdate();
    }

    /**
     * Searches the economy table for a row with matching user ID and returns
     * the ResultSet of the query.
     *
     * @param userId the ID number of the user
     * @return the ResultSet of the query
     */
    private ResultSet getUserRow(long userId) {
        if (!userExists(userId)) addUser(userId);
        return getUserRow(userId, getTable());
    }

    /**
     * Gets a ResultSet containing all rows with expired roles.
     *
     * @return ResultSet containing all rows with expired roles
     * @throws SQLException may be thrown when creating a PreparedStatement
     */
    public ResultSet getExpiredRoles() throws SQLException {
        Date date = new Date();
        return getConnection().prepareStatement("SELECT user, role_colour FROM economy "
                + "WHERE role_expiry > 0 AND role_expiry < " + date.getTime()).executeQuery();
    }

    /**
     * Resets a user's role and expiry date.
     *
     * @param userId the ID number of the user
     * @throws SQLException may be thrown when creating a PreparedStatement
     */
    public void resetUserRole(long userId) throws SQLException {
        getConnection().prepareStatement("UPDATE economy SET role_expiry = 0, role_colour = NULL "
                + "WHERE user = " + userId).executeUpdate();
    }

    /**
     * Searches the economy table for a row with a matching user ID and returns
     * whether or not such a row was found.
     *
     * @param userId the id number of the Discord user being searched for
     * @return true if found, false if not found or error occurs
     */
    private boolean userExists(long userId) {
        return super.userExists(userId, getTable());
    }

    /**
     * Adds a new user to the economy table based on their ID.
     *
     * @param userId the ID number of the new user being added
     */
    @Override
    public void addUser(long userId) {
        try {
            getConnection().prepareStatement("INSERT INTO " + getTable()
                    + " (user, wallet, role_expiry, role_colour)"
                    + " VALUES (" + userId + ", 5, 0, NULL)").executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

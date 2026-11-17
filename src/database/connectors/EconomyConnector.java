package database.connectors;

import database.Connector;
import command.util.economy.RoleListing;
import economy.EconomyService;
import economy.InsufficientFundsException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public final class EconomyConnector extends Connector {

    /**
     * @see Connector
     * Initializes the table to "economy".
     */
    public EconomyConnector() {
        super("economy");
    }

    /**
     * Temporary shim for callers not yet moved to {@link EconomyService}.
     *
     * @deprecated use EconomyService with an explicit operation ID
     */
    @Deprecated
    public void addOrRemoveMoney(long userId, int amount) throws SQLException {
        try {
            if (amount > 0) EconomyService.getInstance().credit(userId, amount, "legacy", null, EconomyService.newOpId());
            else if (amount < 0) EconomyService.getInstance().debit(userId, -amount, "legacy", null, EconomyService.newOpId());
        } catch (InsufficientFundsException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }

    /**
     * Checks if the user can afford a certain expense from spendable coins
     * (the wallet minus anything reserved by an open game or purchase).
     *
     * @param userId the ID number of the user
     * @param cost the cost that is being compared to the user's spendable balance
     * @return true if the user can afford the expense, false otherwise
     * @throws SQLException may be thrown when reading the wallet
     */
    public boolean canAfford(long userId, int cost) throws SQLException {
        return EconomyService.getInstance().getSpendable(userId) >= cost;
    }

    /**
     * Getter for the amount of wealth a user has, including reserved coins.
     *
     * @param userId the ID number of the user
     * @return the wallet balance
     * @throws SQLException may be thrown when reading the wallet
     */
    public int getWealth(long userId) throws SQLException {
        return EconomyService.getInstance().getBalance(userId);
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
    public ResultSet getUserRow(long userId) {
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

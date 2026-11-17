package database.connectors;

import database.Connector;
import command.util.economy.RoleListing;
import economy.EconomyService;

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
     * @param userId the ID number of the user
     * @return the wallet balance minus coins reserved by open games or purchases
     * @throws SQLException may be thrown when reading the wallet
     */
    public int getSpendable(long userId) throws SQLException {
        return EconomyService.getInstance().getSpendable(userId);
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

    /** A rental whose time has run out. */
    public static final class ExpiredRole {
        public final long user;
        public final long roleId;
        public final String roleName;

        ExpiredRole(long user, long roleId, String roleName) {
            this.user = user;
            this.roleId = roleId;
            this.roleName = roleName;
        }
    }

    /**
     * @return every rental past its expiry, read fully before any are processed
     * @throws SQLException may be thrown when creating a PreparedStatement
     */
    public java.util.List<ExpiredRole> getExpiredRoles() throws SQLException {
        java.util.List<ExpiredRole> list = new java.util.ArrayList<>();
        try (ResultSet rs = getConnection().prepareStatement("SELECT user, role_id, role_colour FROM economy "
                + "WHERE role_expiry > 0 AND role_expiry < " + new Date().getTime()).executeQuery()) {
            while (rs.next()) {
                list.add(new ExpiredRole(rs.getLong("user"), rs.getLong("role_id"), rs.getString("role_colour")));
            }
        }
        return list;
    }

    /**
     * @param userId the ID number of the user
     * @return the Discord role ID of the user's current rental, or 0 if none
     * @throws SQLException may be thrown when reading the row
     */
    public long getRoleId(long userId) throws SQLException {
        try (ResultSet rs = getConnection().prepareStatement("SELECT role_id FROM economy WHERE user = " + userId)
                .executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    /**
     * Resets a user's role and expiry date.
     *
     * @param userId the ID number of the user
     * @throws SQLException may be thrown when creating a PreparedStatement
     */
    public void resetUserRole(long userId) throws SQLException {
        getConnection().prepareStatement("UPDATE economy SET role_expiry = 0, role_colour = NULL, role_id = NULL "
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

package database.connectors;

import database.Connector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class RolesConnector extends Connector {

    private String role;
    private ResultSet rs;

    /**
     * @see Connector
     * Initializes table to "roles".
     */
    public RolesConnector() {
        super("roles");
    }

    /**
     * Adds a user application for a role to the database.
     *
     * @param roleName name of the role being applied for
     * @param userId ID number of the user making an application
     * @throws SQLException may be thrown when connecting to the database
     * or handling ResultSets
     */
    public void applyForRole(String roleName, long userId) throws SQLException {
        if (applicationExists(roleName)) {
            System.out.println("exists");
            addExistingRoleApplication(roleName, userId);
        } else {
            System.out.println("doesnt exist");
            addNewRoleApplication(roleName, userId);
        }
    }

    /**
     * Checks if a role exists in the database. Roles
     * exist in the database when they have been requested
     * by at least one user.
     *
     * @param roleName the name of the role being searched for
     * @return true if the role is found in a table
     * @throws SQLException may be thrown when making a prepared statement
     */
    private boolean applicationExists(String roleName) throws SQLException {
        if (role == null || !role.equals(roleName)) setRole(roleName);
        rs.beforeFirst();
        return rs.next();
    }

    /**
     * Adds a new role application to the database with an applicants userId
     * and the role they applied for. This can only be done if no other application
     * to the same role already exists.
     *
     * @param roleName the name of the role being applied for
     * @param userId the ID of the user applying for the role
     * @throws SQLException may be thrown when making a prepared statement
     * or when checking if the role exists
     */
    private void addNewRoleApplication(String roleName, long userId) throws SQLException {
        if (!applicationExists(roleName)) {
            getConnection().prepareStatement("INSERT INTO roles VALUES ('"
                    + roleName + "', " + userId
                    + ", null, null)").executeUpdate();
        }
    }

    /**
     * Checks if a user already applied for a specified role.
     *
     * @param roleName the name of the role the user may have applied for
     * @param userId the ID number of the user who may have made a role application
     * @return true if the user already made a role application, false if they didn't
     * @throws SQLException may be thrown when checking if the role exists or querying a ResultSet
     */
    public boolean userAppliedForRole(String roleName, long userId) throws SQLException {
        if (!applicationExists(roleName)) return false;
        return rs.getFloat("user1") == userId
                || rs.getFloat("user2") == userId
                || rs.getFloat("user3") == userId;
    }

    /**
     * Adds a user to an existing role application list. Does not work
     * on non-existent roles or duplicate applications.
     *
     * @param roleName the name of the role being applied for
     * @param userId the ID number of the user making an application
     * @throws SQLException may be thrown when checking if the role exists
     * or making a prepared statement
     */
    private void addExistingRoleApplication(String roleName, long userId) throws SQLException {
        if (!applicationExists(roleName) || userAppliedForRole(roleName, userId)) return;

        int numApplicants = getNumApplications(roleName);
        if (numApplicants > 0 && numApplicants < 3) {
            getConnection().prepareStatement("UPDATE roles"
                    + " SET user" + (numApplicants + 1) + " = " + userId
                    + " WHERE name = '" + roleName + "'").executeUpdate();
        }
    }

    /**
     * Removes a user's application for a role from the database.
     *
     * @param roleName the name of the role
     * @param userId the user's ID number
     * @throws SQLException may be thrown when handling ResultSets
     * @see command.commands.roles.Leave
     */
    public void removeApplication(String roleName, long userId) throws SQLException {
        if (!applicationExists(roleName) || !userAppliedForRole(roleName, userId)) return;
        for (int i = 1; i < 4; i++) {
            if (rs.getFloat("user" + i) == userId) {
                getConnection().prepareStatement("UPDATE roles SET user" + i + " = null "
                        + "WHERE name = '" + roleName + "'").executeUpdate();

                for (int j = i + 1; j < 4; j++) {
                    getConnection().prepareStatement("UPDATE roles SET user" + (j - 1) + " = user" + j
                            + " WHERE name = '" + roleName + "'").executeUpdate();
                }

                setRole(roleName);
                if (rs.getFloat("user1") == 0) {
                    getConnection().prepareStatement("DELETE FROM roles "
                            + "WHERE name = '" + roleName + "'").executeUpdate();
                }

                break;
            }
        }
    }

    /**
     * Returns the number of people who applied for a specific role.
     *
     * @param roleName the name of the role being applied for
     * @return the number of people who applied for it (max of 3)
     * @throws SQLException may be thrown when checking if the role exists
     * or when querying a ResultSet
     */
    public int getNumApplications(String roleName) throws SQLException {
        if (!applicationExists(roleName)) return 0;
        for (int i = 1; i <= 3; i++) {
            if (rs.getLong("user" + i) == 0) {
                return i - 1;
            }
        }
        throw new SQLException("Acquired too many columns");
    }

    /**
     * Returns an ArrayList of the user ID numbers of every
     * applicant for a specified role.
     *
     * @param roleName the name of the role
     * @return an ArrayList of the user ID numbers of every
     * applicant for that role
     * @throws SQLException may be thrown when checking if the role exists
     * or when querying a ResultSet
     */
    public ArrayList<Long> getApplicantIds(String roleName) throws SQLException {
        ArrayList<Long> applicants = new ArrayList<>();
        if (!applicationExists(roleName)) return applicants;
        for (int i = 1; i < 4; i++) {
            applicants.add(rs.getLong("user" + i));
        }
        return applicants;
    }

    /**
     * Sets the instance variables to the role being manipulated/queried.
     *
     * @param roleName name of the role
     * @throws SQLException may be thrown when making a prepared statement
     */
    private void setRole(String roleName) throws SQLException {
        rs = getConnection().prepareStatement("SELECT * FROM " + getTable()
                + " WHERE name = '" + roleName + "'").executeQuery();
        role = roleName;
        rs.first();
    }

    /**
     * Has no functionality in this connector.
     *
     * @param userId ignored
     */
    @Override
    public void addUser(long userId) {
        System.out.println("Cannot add lone user to this table");
    }
}

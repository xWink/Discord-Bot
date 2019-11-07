package database;

import main.RoleBot;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Abstraction of the connection between the bot and the database.
 * Contains Connection instance variable that is utilized by subclasses.
 */
abstract class Connector {

    private static Connection connection;


    Connector() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://localhost/discord_bot",
                    RoleBot.config.db_user,
                    RoleBot.config.db_pass);
        }
        catch (Exception e){
            System.out.println("Failed to connect to database. Shutting down...");
            System.exit(-1);
        }
    }


    /**
     * Connection getter.
     *
     * @return the database connection
     */
    Connection getConnection() {
        return connection;
    }


    /**
     * Searches the table for a row with a matching user ID and returns whether
     * or not such a row was found.
     *
     * @param table the name of the table being queried
     * @param userId the id number of the Discord user being searched for
     * @return true if found, false if not found or error occurs
     */
    boolean userExistsInTable(String table, long userId) {
        try {
            PreparedStatement checkIfExists = connection.prepareStatement("SELECT * FROM " + table
                    + " WHERE user = " + userId);
            ResultSet rs = checkIfExists.executeQuery();
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Searches the table for a row with matching user ID and returns
     * the ResultSet of the query.
     *
     * @param table the name of the table being queried
     * @param userId the id number of the Discord user being searched for
     * @return the ResultSet of the query
     */
    ResultSet getUserRow(String table, long userId) {
        try {
            return connection.prepareStatement("SELECT * FROM " + table
                    + " WHERE user = " + userId).executeQuery();
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Adds a new user to a specified ta a table based on their ID
     *
     * @param userId the ID number of the new user being added
     */
    abstract void addUser(long userId);
}

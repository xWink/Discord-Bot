package DiscordBot.util.database;

import DiscordBot.RoleBot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseUtil {

    private static Connection conn;

    static {
        conn = connect();
    }

    private static Connection connect() {
        // Connect to database
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            return DriverManager.getConnection("jdbc:mysql://localhost/discord_bot",
                    RoleBot.config.db_user,
                    RoleBot.config.db_pass);
        }
        catch (Exception e){
            System.out.println("blackjack Exception 1\nException: "+ e.toString());
            return null;
        }
    }

    public static Connection getConnection() {
        return conn;
    }

    public static boolean userExistsInTable(String tableName, long userId) {
        try {
            PreparedStatement checkIfExists = conn.prepareStatement("SELECT * FROM " + tableName
                    + " WHERE user = " + userId);
            ResultSet rs = checkIfExists.executeQuery();
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }

    public static ResultSet getUserRowInTable(String tableName, long userId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + tableName
                + " WHERE user = " + userId);
        return ps.executeQuery();
    }
}

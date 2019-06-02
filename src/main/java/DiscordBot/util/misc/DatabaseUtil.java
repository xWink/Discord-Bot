package DiscordBot.util.misc;

import DiscordBot.RoleBot;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseUtil {

    public static Connection connect(){

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
}

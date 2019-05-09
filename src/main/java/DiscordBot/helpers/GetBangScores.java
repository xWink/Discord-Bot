package DiscordBot.helpers;

import DiscordBot.RoleBot;
import net.dv8tion.jda.core.entities.Guild;

import java.util.Date;

import java.sql.*;

public class GetBangScores {

    public static BangHighScores getBangScores(Guild guild){

        double attemptCount, deathCount, bestRate, worstRate;
        String mostAttemptsPlayer, mostDeathsPlayer, luckiestPlayer, unluckiestPlayer;
        Date date = new Date();
        Connection conn;
        ResultSet mostAttempts, mostDeaths, luck;

        // Connect to database
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost/discord_bot", RoleBot.config.db_user, RoleBot.config.db_pass);
        }
        catch (Exception e){
            System.out.println("GetBangScores Exception 1");
            System.out.println("Exception: "+ e.toString());
            System.out.println("Failed to connect to database, terminating command");
            return null;
        }

        // Find high score holders who have played within the last week
        try {
 System.out.println("here1");
            // Most attempts
            PreparedStatement getMostAttempts = conn.prepareStatement("SELECT CAST user AS NUMERIC, tries FROM bang WHERE "+date.getTime()+" - last_played < 604800000 GROUP BY user, tries ORDER BY tries");
            mostAttempts = getMostAttempts.executeQuery();
            mostAttempts.last();
            System.out.println("tries: "+mostAttempts.getDouble("tries"));
            System.out.println("user: "+mostAttempts.getDouble("user"));
            attemptCount = mostAttempts.getDouble("tries");
            mostAttemptsPlayer = guild.getMemberById((long)mostAttempts.getDouble("user")).getUser().getName();
 System.out.println("Here2");
            // Most deaths
            PreparedStatement getMostDeaths = conn.prepareStatement("SELECT user, deaths FROM bang WHERE "+date.getTime()+" - last_played < 604800000 GROUP BY user, deaths ORDER BY deaths");
            mostDeaths = getMostDeaths.executeQuery();
            mostDeaths.next();
            deathCount = mostDeaths.getDouble("deaths");
            mostDeathsPlayer = guild.getMemberById((long)mostDeaths.getDouble("user")).getUser().getName();
 System.out.println("here3");
            // Get luckiest and unluckiest players
            PreparedStatement getLuckRanks = conn.prepareStatement("SELECT user, death_rate FROM bang WHERE "+date.getTime()+" - last_played < 604800000 GROUP BY user, death_rate ORDER BY death_rate");
            luck = getLuckRanks.executeQuery();

            // Luckiest
            luck.first();
            bestRate = 100 - (Math.round(luck.getDouble("death_rate") * 10d) / 10d);
            luckiestPlayer = guild.getMemberById((long)luck.getDouble("user")).getUser().getName();

            // Unluckiest
            luck.last();
            worstRate = 100 - (Math.round(luck.getDouble("death_rate") * 10d) / 10d);
            unluckiestPlayer = guild.getMemberById((long)luck.getDouble("user")).getUser().getName();
        }
        catch (Exception e) {
            System.out.println("GetBangScores Exception 2");
            e.printStackTrace();
            return null;
        }
        System.out.println("returning");

        return new BangHighScores(
                attemptCount,
                mostAttemptsPlayer,
                deathCount,
                mostDeathsPlayer,
                bestRate,
                luckiestPlayer,
                worstRate,
                unluckiestPlayer);
    }
}

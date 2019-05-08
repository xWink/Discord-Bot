package DiscordBot.helpers;

import net.dv8tion.jda.core.entities.Guild;

import java.util.Date;

import java.sql.*;

public class GetBangScores {

    public static BangHighScores getBangScores(Guild guild){

        double attemptCount, deathCount, bestRate, worstRate;
        String mostAttemptsPlayer, mostDeathsPlayer, luckiestPlayer, unluckiestPlayer;
        Date date = new Date();
        Connection conn;
        ResultSet mostAttempts, mostDeaths, luckiest, unluckiest;

        // Connect to database
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost/discord_bot", "admin", "xFc6zgmQ");
        }
        catch (Exception e){
            System.out.println("GetBangScores Exception 1");
            System.out.println("Exception: "+ e.toString());
            System.out.println("Failed to connect to database, terminating command");
            return null;
        }

        // Find high score holders who have played within the last week
        try {
            // Most attempts
            PreparedStatement getMostAttempts = conn.prepareStatement("SELECT *, MAX(tries) FROM bang WHERE "+date.getTime()+" - last_played < 604800000 GROUP BY user, deaths, last_played, death_rate");
            mostAttempts = getMostAttempts.executeQuery();
            mostAttempts.next();
            attemptCount = mostAttempts.getDouble("tries");
            mostAttemptsPlayer = guild.getMemberById((long)mostAttempts.getDouble("user")).getUser().getName();

            // Most deaths
            PreparedStatement getMostDeaths = conn.prepareStatement("SELECT *, MAX(deaths) FROM bang WHERE "+date.getTime()+" - last_played < 604800000 GROUP BY user, tries, last_played, death_rate");
            mostDeaths = getMostDeaths.executeQuery();
            mostDeaths.next();
            deathCount = mostDeaths.getDouble("deaths");
            mostDeathsPlayer = guild.getMemberById((long)mostDeaths.getDouble("user")).getUser().getName();

            // Luckiest
            PreparedStatement getLuckiest = conn.prepareStatement("SELECT *, MIN(death_rate) FROM bang WHERE "+date.getTime()+" - last_played < 604800000 GROUP BY user, tries, deaths, last_played");
            luckiest = getLuckiest.executeQuery();
            luckiest.next();
            bestRate = Math.round(100 - (mostDeaths.getDouble("death_rate") * 10d)) / 10d;
            luckiestPlayer = guild.getMemberById((long)luckiest.getDouble("user")).getUser().getName();

            // Unluckiest
            PreparedStatement getUnluckiest = conn.prepareStatement("SELECT *, MAX(death_rate) FROM bang WHERE "+date.getTime()+" - last_played < 604800000 GROUP BY user, tries, deaths, last_played");
            unluckiest = getUnluckiest.executeQuery();
            unluckiest.next();
            worstRate = Math.round(100 - (mostDeaths.getDouble("death_rate") * 10d)) / 10d;
            unluckiestPlayer = guild.getMemberById((long)unluckiest.getDouble("user")).getUser().getName();
        }
        catch (SQLException e) {
            System.out.println("GetBangScores Exception 2");
            e.printStackTrace();
            return null;
        }

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

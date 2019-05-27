package DiscordBot.commands.Bang;

import DiscordBot.RoleBot;
import net.dv8tion.jda.core.entities.Guild;

import java.util.Date;

import java.sql.*;

class GetBangScores {

    static BangHighScores getBangScores(Guild guild){

        double attemptCount, deathCount, bestRate, worstRate;
        int jamCount;
        String mostAttemptsPlayer, mostDeathsPlayer, luckiestPlayer, unluckiestPlayer, mostJamsPlayer;
        Date date = new Date();
        Connection conn;
        ResultSet mostAttempts, mostDeaths, luck, mostJams;

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
            // Most attempts
            PreparedStatement getMostAttempts = conn.prepareStatement("SELECT user, tries FROM bang WHERE "+date.getTime()+" - last_played < 604800000 GROUP BY user, tries ORDER BY tries");
            mostAttempts = getMostAttempts.executeQuery();
            mostAttempts.last();
            attemptCount = mostAttempts.getDouble("tries");
            mostAttemptsPlayer = guild.getMemberById(mostAttempts.getLong("user")).getUser().getName();

            // Most deaths
            PreparedStatement getMostDeaths = conn.prepareStatement("SELECT user, deaths FROM bang WHERE "+date.getTime()+" - last_played < 604800000 GROUP BY user, deaths ORDER BY deaths");
            mostDeaths = getMostDeaths.executeQuery();
            mostDeaths.last();
            deathCount = mostDeaths.getDouble("deaths");
            mostDeathsPlayer = guild.getMemberById(mostDeaths.getLong("user")).getUser().getName();

            // Get luckiest and unluckiest players
            PreparedStatement getLuckRanks = conn.prepareStatement("SELECT user, death_rate FROM bang WHERE "+date.getTime()+" - last_played < 604800000 AND tries > 20 GROUP BY user, death_rate ORDER BY death_rate");
            luck = getLuckRanks.executeQuery();

            // Luckiest
            luck.first();
            bestRate = 100 - (Math.round(luck.getDouble("death_rate") * 10d) / 10d);
            luckiestPlayer = guild.getMemberById(luck.getLong("user")).getUser().getName();

            // Unluckiest
            luck.last();
            worstRate = 100 - (Math.round(luck.getDouble("death_rate") * 10d) / 10d);
            unluckiestPlayer = guild.getMemberById(luck.getLong("user")).getUser().getName();

            // Most jams
            PreparedStatement getMostJams = conn.prepareStatement("SELECT user, jams FROM bang WHERE "+date.getTime()+" - last_played < 604800000 GROUP BY user, jams ORDER BY jams");
            mostJams = getMostJams.executeQuery();
            mostJams.last();
            jamCount = mostJams.getInt("jams");
            mostJamsPlayer = guild.getMemberById(mostJams.getLong("user")).getUser().getName();
        }
        catch (Exception e) {
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
                unluckiestPlayer,
                jamCount,
                mostJamsPlayer);
    }
}

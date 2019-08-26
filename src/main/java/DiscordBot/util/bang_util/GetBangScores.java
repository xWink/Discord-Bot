package DiscordBot.util.bang_util;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;

import java.util.Date;

import java.sql.*;

import static DiscordBot.util.misc.DatabaseUtil.connect;

public class GetBangScores {

    public static BangHighScores getHighScores(Guild guild){

        double attemptCount, bestRate, worstRate;
        int wealth;
        User mostAttemptsPlayer, luckiestPlayer, unluckiestPlayer, wealthiestPlayer;
        Date date = new Date();
        Connection conn;
        ResultSet mostAttempts, luck, mostWealth;

        // Connect to database
        if ((conn = connect()) == null){
            System.out.println("Could not connect to database. Please contact a moderator :(");
            return null;
        }

        // Find high score holders who have played within the last week
        try {
            // Most attempts
            PreparedStatement getMostAttempts = conn.prepareStatement("SELECT user, tries FROM bang WHERE "+date.getTime()+" - last_played < 604800000 GROUP BY user, tries ORDER BY tries");
            mostAttempts = getMostAttempts.executeQuery();
            mostAttempts.last();
            attemptCount = mostAttempts.getDouble("tries");
            mostAttemptsPlayer = guild.getMemberById(mostAttempts.getLong("user")).getUser();

            // Get luckiest and unluckiest players
            PreparedStatement getLuckRanks = conn.prepareStatement("SELECT user, death_rate FROM bang WHERE "+date.getTime()+" - last_played < 604800000 AND tries > 20 GROUP BY user, death_rate ORDER BY death_rate");
            luck = getLuckRanks.executeQuery();

            // Luckiest
            luck.first();
            bestRate = 100 - (Math.round(luck.getDouble("death_rate") * 10d) / 10d);
            luckiestPlayer = guild.getMemberById(luck.getLong("user")).getUser();

            // Unluckiest
            luck.last();
            worstRate = 100 - (Math.round(luck.getDouble("death_rate") * 10d) / 10d);
            unluckiestPlayer = guild.getMemberById(luck.getLong("user")).getUser();

            // Wealthiest
            PreparedStatement getWealthiest = conn.prepareStatement("SELECT user, wallet FROM economy GROUP BY user, wallet ORDER BY wallet");
            mostWealth = getWealthiest.executeQuery();
            mostWealth.last();
            wealth = mostWealth.getInt("wallet");
            wealthiestPlayer = guild.getMemberById(mostWealth.getLong("user")).getUser();
        }
        catch (Exception e) {
            System.out.println("GetBangScores Exception 2");
            e.printStackTrace();
            return null;
        }

        return new BangHighScores(
                attemptCount,
                mostAttemptsPlayer,
                bestRate,
                luckiestPlayer,
                worstRate,
                unluckiestPlayer,
                wealth,
                wealthiestPlayer);
    }

    public static int[] getTotalBangs(Connection conn){

        int[] values = {0,0};

        try {
            ResultSet rs = conn.prepareStatement("SELECT SUM(tries), SUM(deaths) FROM bang").executeQuery();
            if (rs.next()){
                values[0] = rs.getInt("SUM(tries)");
                values[1] = rs.getInt("SUM(deaths)");
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }

        return values;
    }
}

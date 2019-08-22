package DiscordBot.commands.bang;

import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Daily {

    // Acquire user's daily time from database
    private static long getDaily(User author, Connection conn, MessageChannel channel){

        try{
            PreparedStatement st = conn.prepareStatement("SELECT last_daily FROM bang WHERE user="+author.getIdLong());
            ResultSet rs = st.executeQuery();
            rs.next();

            // Add 24 hours and 1 minute to the time
            return rs.getLong("last_daily") + 86460000;
        }
        catch (SQLException e){
            channel.sendMessage("Error fetching your daily time, please contact a moderator!").queue();
        }

        return -1;
    }

    // Output that time as a date/time
    public static void daily(User author, Connection conn, MessageChannel channel) {

        SimpleDateFormat df = new SimpleDateFormat("h:mm a"); // Set format of date/time
        TimeZone zone = TimeZone.getTimeZone("America/New_York"); // Get timezone
        df.setTimeZone(zone); // Apply timezone to format
        long resetTime = getDaily(author, conn, channel); // Get reset time

        if (resetTime == -1) // Check if error returned
            channel.sendMessage("Error, please contact a moderator!").queue();
        else if (resetTime <= new Date().getTime()) // Check if reward is available now
            channel.sendMessage("Your daily reward is available now!").queue();
        else // Output when reward is available
            channel.sendMessage("Your next daily reward is available at "+df.format(new Date(resetTime))).queue();
    }
}

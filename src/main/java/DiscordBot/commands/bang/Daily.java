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

            // Add 24 hours to the time
            return rs.getLong("last_daily") + 86400000;
        }
        catch (SQLException e){
            channel.sendMessage("Error fetching your daily time, please contact a moderator!").queue();
        }

        return -1;
    }

    // Output that time as a date/time
    public static void daily(User author, Connection conn, MessageChannel channel) {

        // Format acquired date to desired style
        SimpleDateFormat df = new SimpleDateFormat("MMM dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("EST"));
        Date date = new Date(getDaily(author, conn, channel));

        channel.sendMessage("Your next daily reward is available on: "+df.format(date)).queue();
    }
}

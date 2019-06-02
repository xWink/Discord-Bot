package DiscordBot.commands.bang;

import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.sql.*;

public class MyBang {

    public static void myBang(User author, MessageChannel channel, Connection conn){

        ResultSet rs = null;
        Boolean exists = false;

        // Find user in database
        try {
            PreparedStatement st = conn.prepareStatement("SELECT * FROM bang WHERE user="+author.getIdLong());
            rs = st.executeQuery();
            if (rs.next())
                exists = true;
        }
        catch (SQLException e) {
            System.out.println("MyBang Exception 2");
            System.out.println("SQL Exception: "+ e.toString());
        }

        if (!exists)
            channel.sendMessage(author.getName()+" could not be found in score list").queue();
        else{
            try {
                // Post results
                double survivalRate = 100 - Math.round(rs.getDouble("deaths") / rs.getDouble("tries") * 100 * 10d) / 10d;
                channel.sendMessage("**" + author.getName() +
                        "'s scores**\nAttempts: " + (int) rs.getDouble("tries") +
                        "\nDeaths: " + (int) rs.getDouble("deaths") +
                        "\nSurvival rate: " + survivalRate + "%").complete();
                if (rs.getInt("jams") > 0)
                    channel.sendMessage("Jams: "+rs.getInt("jams")).queue();
            }
            catch (SQLException e){
                System.out.println("MyBang Exception 3");
                System.out.println("Exception: "+ e.toString());
            }
        }
    }
}

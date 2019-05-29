package DiscordBot.commands.misc;

import DiscordBot.RoleBot;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;

import java.sql.*;

public class Ping {

	public static void ping(User author, Event event, MessageChannel channel){

		Connection conn;
		ResultSet rs = null;
		long ping = event.getJDA().getPing();

		try {
			Class.forName("org.mariadb.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost/discord_bot", RoleBot.config.db_user, RoleBot.config.db_pass);
		}
		catch (Exception e){
			System.out.println("Ping Exception 1");
			System.out.println("Exception: "+ e.toString());
			System.out.println("Failed to connect to database, terminating command");
			return;
		}

		try{
			PreparedStatement st = conn.prepareStatement("SELECT * FROM ping WHERE user="+author.getIdLong());
			rs = st.executeQuery();
		}
		catch (SQLException e){
			System.out.println("Ping Exception 2");
			System.out.println("SQL Exception: "+ e.toString());
		}

		channel.sendMessage("Pong! " + ping + " ms").queue();

		try {
			PreparedStatement st;
			if (rs == null || !rs.next()){
				st = conn.prepareStatement("INSERT INTO ping VALUES('"+author.getIdLong()+"', "+ping+", "+ping+")");
				st.executeUpdate();
			}
			else if (ping > rs.getInt("max")){
				channel.sendMessage("Lul, that's your worst ping so far").queue();
				st = conn.prepareStatement("UPDATE ping SET max = "+ping+" WHERE user = "+author.getIdLong());
				st.executeUpdate();
			}
			else if (ping < rs.getInt("min")){
				channel.sendMessage("Wow, a new personal best!").queue();
				st = conn.prepareStatement("UPDATE ping SET min = "+ping+" WHERE user = "+author.getIdLong());
				st.executeUpdate();
			}
		}
		catch (SQLException e) {
			System.out.println("Ping Exception 3");
			e.printStackTrace();
		}
	}
}

package DiscordBot.commands.Bang;

import DiscordBot.RoleBot;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.sql.*;
import java.util.Date;
import java.util.Random;
import java.lang.Math;

public class Roulette {

	public static int roulette(User author, int chamberCount, MessageChannel channel){

		Random rand = new Random();
		Date date = new Date();
		Connection conn;

		// Connect to database
		try {
			Class.forName("org.mariadb.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost/discord_bot", RoleBot.config.db_user, RoleBot.config.db_pass);
		}
		catch (Exception e){
			System.out.println("Roulette Exception 1");
			System.out.println("Exception: "+ e.toString());
			System.out.println("Failed to connect to database, terminating command");
			channel.sendMessage("An error occurred, please contact a moderator :(").queue();
			return chamberCount;
		}

		// Calculate whether the user died
		int pull = rand.nextInt(chamberCount);
		int boom, jammed = 0;

		// When pull == 0, the gun is supposed to go boom
		if (pull == 0) {
			// If there is one chamber left
			if (chamberCount == 1) {
				// There is a 1/10 chance of the gun jamming
				int jam = 1 + (int)(Math.random() * 9);
				// If the gun jams
				if (jam == 4) {
					boom = 0;
					chamberCount = 6;
					jammed = 1;
					channel.sendMessage("The gun jammed... " + author.getName() + " survived <:poggers:564285288621539328> <:poggers:564285288621539328> <:poggers:564285288621539328>").complete();
				}
				// If the gun doesn't jam with one chamber left, boom
				else {
					boom = 1;
					chamberCount = 6;
					channel.sendMessage("Bang! " + author.getName() + " died :skull:").complete();
				}
			}
			// If there is more than 1 chamber left, boom
			else {
				boom = 1;
				chamberCount = 6;
				channel.sendMessage("Bang! " + author.getName() + " died :skull:").complete();
			} 
		}
		// No boom
		else {
			boom = 0;
			chamberCount--;
			channel.sendMessage("Click. " + author.getName() + " survived  <:poggies:564285288621539328>").complete();
		}

		channel.sendMessage("Chambers left in the cylinder: ||  "+chamberCount+"  ||").complete();


		// Find user in database
		try {
			Boolean exists = false;
			PreparedStatement st = conn.prepareStatement("SELECT * FROM bang WHERE user="+author.getIdLong());
			ResultSet rs = st.executeQuery();

			if(rs.next()){
				exists = true;
			}

			// If user doesn't exist, add new user
			if (!exists){
				PreparedStatement stmt = conn.prepareStatement("INSERT INTO bang (user, tries, deaths, jams, last_played) VALUES ("+author.getIdLong()+", 1, "+boom+", "+jammed+", "+date.getTime()+")");
				stmt.executeUpdate();
			}

			// If user exists, update the scores based on boom and jammed value
			else {
				Statement stmt = conn.createStatement();
				if (boom == 1)
					stmt.executeUpdate("UPDATE bang SET tries = tries + 1, deaths = deaths + 1, last_played = " + date.getTime() + " WHERE user = " + author.getIdLong());
				else if (jammed == 1)
					stmt.executeUpdate("UPDATE bang SET tries = tries + 1, jams = jams + 1, last_played = " + date.getTime() + " WHERE user = " + author.getIdLong());
				else
					stmt.executeUpdate("UPDATE bang SET tries = tries + 1, last_played = " + date.getTime() + " WHERE user = " + author.getIdLong());
			}
		}
		catch (SQLException e) {
			System.out.println("Roulette Exception 2");
			System.out.println("SQL Exception: "+ e.toString());
			channel.sendMessage("An error occurred, please contact a moderator :(").queue();
		}

		return chamberCount;
	}
}

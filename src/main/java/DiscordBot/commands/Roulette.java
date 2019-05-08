package DiscordBot.commands;

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
			conn = DriverManager.getConnection("jdbc:mysql://localhost/discord_bot", "admin", "xFc6zgmQ");
		}
		catch (Exception e){
			System.out.println("Roulette Exception 1");
			System.out.println("Exception: "+ e.toString());
			System.out.println("Failed to connect to database, terminating command");
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
					channel.sendMessage("The gun jammed... " + author.getName() + " survived <:poggers:564285288621539328> <:poggers:564285288621539328> <:poggers:564285288621539328>").queue();
				}
				// If the gun doesn't jam with one chamber left, boom
				else {
					boom = 1;
					chamberCount = 6;
					channel.sendMessage("Bang! " + author.getName() + " died :skull:").queue();
				}
			}
			// If there is more than 1 chamber left, boom
			else {
				boom = 1;
				chamberCount = 6;
				channel.sendMessage("Bang! " + author.getName() + " died :skull:").queue();
			} 
		}
		// No boom
		else {
			boom = 0;
			chamberCount--;
			channel.sendMessage("Click. " + author.getName() + " survived  <:poggies:564285288621539328>").queue();
		}

		channel.sendMessage("Chambers left in the cylinder: ||  "+chamberCount+"  ||").queue();


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
				Statement stmt = conn.createStatement();
				stmt.executeUpdate("INSERT INTO bang VALUES ('"+author.getId()+"', 1, "+boom+", "+jammed+", "+date.getTime()+")");
			}

			// If user exists, update the scores based on boom and jammed value
			else {
				Statement stmt = conn.createStatement();
				if (boom == 1)
					stmt.executeUpdate("UPDATE bang SET tries = tries + 1, deaths = deaths + 1, last_played = " + date.getTime() + " WHERE user = " + author.getIdLong());
				else if (jammed == 1)
					stmt.executeUpdate("UPDATE bang SET tries = tries + 1, jammed = jammed + 1, last_played = " + date.getTime() + " WHERE user = " + author.getIdLong());
				else
					stmt.executeUpdate("UPDATE bang SET tries = tries + 1, last_played = " + date.getTime() + " WHERE user = " + author.getIdLong());
			}
		}
		catch (SQLException e) {
			System.out.println("Roulette Exception 2");
			System.out.println("SQL Exception: "+ e.toString());
		}

		return chamberCount;
	}
}

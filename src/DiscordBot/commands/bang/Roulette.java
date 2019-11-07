package DiscordBot.commands.bang;

import DiscordBot.util.bang_util.BangHighScores;
import DiscordBot.util.bang_util.GetBangScores;
import DiscordBot.util.economy.Wallet;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.sql.*;
import java.util.Date;
import java.util.Random;
import java.lang.Math;

public class Roulette {

	private static boolean eligibleForReward(ResultSet rs) throws SQLException{

		Date date = new Date();
		return date.getTime() - rs.getLong("last_daily") >= 86400000;
	}

	private static void giveDailyReward(User author, Connection conn, TextChannel channel) throws SQLException{

		Date date = new Date();
		BangHighScores highScores = GetBangScores.getHighScores(channel.getGuild());
		int reward = 5;
		boolean hasHighscore = false;

		// Check if user is a high score holder
		if (highScores != null) {
			if (author.equals(highScores.getLuckiest()) ||
					author.equals(highScores.getMostAttemptsPlayer()) ||
					author.equals(highScores.getWealthiest()) ||
					author.equals(highScores.getUnluckiest())) {
				hasHighscore = true;
			}
		}

		// Give user daily reward money
		Wallet wallet = new Wallet(author, conn);

		if (hasHighscore) {
			wallet.addMoney(conn, reward * 2);
			channel.sendMessage(author.getName() +
					" received their daily reward of " + reward + " GryphCoins\n***BONUS***  " + reward +
					" GryphCoins for having a bang high score!").complete();
		}
		else{
			wallet.addMoney(conn, reward);
			channel.sendMessage(author.getName() +
					" received their daily reward of " + reward + " GryphCoins!").complete();
		}

		conn.prepareStatement("UPDATE bang SET last_daily = " + date.getTime() + " WHERE user = " + author.getIdLong()).executeUpdate();
	}

	public static int roulette(User author, int chamberCount, TextChannel channel, Connection conn){

		Random rand = new Random();
		Date date = new Date();
		String poggers = "<:poggers:564285288621539328>";

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
					channel.sendMessage("The gun jammed... " +
							author.getName() + " survived " + poggers + poggers + poggers).complete();
				}
				// If the gun doesn't jam with one chamber left, boom
				else {
					boom = 1;
					chamberCount = 6;
					channel.sendMessage("bang! " + author.getName() + " died :skull:").complete();
				}
			}
			// If there is more than 1 chamber left, boom
			else {
				boom = 1;
				chamberCount = 6;
				channel.sendMessage("bang! " + author.getName() + " died :skull:").complete();
			} 
		}
		// No boom
		else {
			boom = 0;
			chamberCount--;
			channel.sendMessage("Click. "+author.getName()+" survived  <:poggies:564285288621539328>").complete();
		}

		channel.sendMessage("Chambers left in the cylinder: ||  "+chamberCount+"  ||").complete();

		// Find user in database
		try {
			boolean exists = false;
			PreparedStatement st = conn.prepareStatement("SELECT * FROM bang WHERE user="+author.getIdLong());
			ResultSet rs = st.executeQuery();

			if(rs.next()){
				exists = true;
			}

			// If user doesn't exist, add new user
			if (!exists){
				conn.prepareStatement("INSERT INTO bang (user, tries, deaths, jams, last_played) VALUES (" +
						author.getIdLong()+", 1, " + boom + ", " + jammed + ", " + date.getTime() + ")").executeUpdate();

				giveDailyReward(author, conn, channel);
			}

			// If user exists, update the scores based on boom and jammed value and give daily rewards
			else {
				if (eligibleForReward(rs))
					giveDailyReward(author, conn, channel);

				Statement stmt = conn.createStatement();
				if (boom == 1)
					stmt.executeUpdate("UPDATE bang SET tries = tries + 1, deaths = deaths + 1, last_played = " +
							date.getTime() + " WHERE user = " + author.getIdLong());
				else if (jammed == 1) {
					stmt.executeUpdate("UPDATE bang SET tries = tries + 1, jams = jams + 1, last_played = " +
							date.getTime() + " WHERE user = " + author.getIdLong());

					Wallet wallet = new Wallet(author, conn);
					wallet.addMoney(conn, 50);
					channel.sendMessage(author.getName() + " received a bonus 50 GryphCoins!").complete();
				}
				else
					stmt.executeUpdate("UPDATE bang SET tries = tries + 1, last_played = " +
							date.getTime() + " WHERE user = " + author.getIdLong());
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

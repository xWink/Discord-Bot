package DiscordBot.commands;

import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.DateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.lang.Math;

public class Roulette {

	public static int roulette(User author, String path3, int chamberCount, MessageChannel channel){

		Random rand = new Random();
		Date date = new Date();
		Connection conn = null;

		//File file = new File(path3);
		// Calculate whether the user died
		int pull = rand.nextInt(chamberCount);
		int boom, jammed = 0;

		// Connect to database
		try {
			Class.forName("org.mariadb.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost/discord_bot", "admin", "xFc6zgmQ");
		}
		catch (Exception e){
			e.printStackTrace();
		}
		if (conn == null){
			System.out.println("Failed to connect to database, terminating command");
			return chamberCount;
		}

		if (pull == 0) {
			// If there is one bullet left, there is a 1/10 chance of the gun jamming
			if (chamberCount == 1) {
				int jam = 1 + (int)(Math.random() * 9);

				if (jam == 4) {
					boom = 0;
					chamberCount = 6;
					jammed = 1;
					channel.sendMessage("The gun jammed... " + author.getName() + " survived <:poggers:564285288621539328>").queue();
				}
				
				// If the gun doesn't jam
				else {
					boom = 1;
					chamberCount = 6;
					channel.sendMessage("Bang! " + author.getName() + " died :skull:").queue();
				}
			} 
			
			// >1 bullets left
			else {
				boom = 1;
				chamberCount = 6;
				channel.sendMessage("Bang! " + author.getName() + " died :skull:").queue();
			} 
		}
		
		// User doesn't die
		else {
			boom = 0;
			chamberCount--;
			channel.sendMessage("Click. " + author.getName() + " survived  <:poggies:564285288621539328>").queue();
		}

		channel.sendMessage("Chambers left in the cylinder: ||  "+chamberCount+"  ||").queue();


		// Find user in database
		Boolean exists = false;
		try {
			PreparedStatement st = conn.prepareStatement("SELECT * FROM items WHERE user="+author.getIdLong());
			ResultSet rs = st.executeQuery();

			if(rs != null){
				System.out.println("It already exists");
				exists = true;
			}
		}
		catch (SQLException e) {
			System.out.println("SQL Exception: "+ e.toString());
		}

		// If user doesn't exist, add new user
		if (!exists){
			try{
				Statement stmt = conn.createStatement();
				stmt.executeUpdate("INSERT INTO bang VALUES ('"+author.getIdLong()+"', 1, "+boom+", "+jammed+", "+date.getTime()+")");
			}
			catch (SQLException e){
				System.out.println("SQL Exception: "+ e.toString());
			}
		}
		System.out.println("Success");

		// If user exists, update the scores based on boom and chamberCount value

		/*
		try {
			// Create writers, readers, boolean, etc
			Path filePath = Paths.get(path3);
			BufferedReader reader = Files.newBufferedReader(filePath);
			boolean found = false;
			int attempts;
			int deaths;

			// Get number of lines
			BufferedReader bufferedReader = Files.newBufferedReader(filePath);
			int lineCount = 0;
			int i = 0;
			String line;
			while (bufferedReader.readLine() != null) {
				lineCount++;
			}

			// Store file content in array
			String[] fileContent = new String[lineCount];
			while ((line = reader.readLine()) != null) {
				fileContent[i] = line + "\n";
				i++;
			}

			// Find matching username
			for (i = 0; i < lineCount; i++){
				if (fileContent[i].startsWith("\""+author.getId()+"\"")){
					found = true;

					// Convert trigger pulls and deaths to ints and increase accordingly
					attempts = Integer.parseInt(fileContent[i].substring(fileContent[i].indexOf("\",\"")+3,fileContent[i].indexOf("\"", fileContent[i].indexOf("\",\"")+3)));
					deaths = Integer.parseInt(fileContent[i].substring(fileContent[i].indexOf("\",\"", fileContent[i].indexOf("\",\"")+3)+3, fileContent[i].length()-2));
					attempts++;
					deaths += boom;

					// Rewrite the line in fileContent with new numbers
					fileContent[i] = "\""+author.getId()+"\",\""+Integer.toString(attempts)+"\",\""+Integer.toString(deaths)+"\"\n";
					break;
				}
			}

			// Create writers to append
			FileWriter fw = new FileWriter(path3, true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter appendWriter = new PrintWriter(bw);

			// If user not found, add new name to file
			if (!found){
				String newPlayer = "\""+author.getId()+"\",\"1\",\""+Integer.toString(boom)+"\"\n";
				appendWriter.append(newPlayer);
			}
			else {
				// Erase file content
				PrintWriter printWriter = new PrintWriter(file);
				printWriter.write("");
				printWriter.close();

				// Rewrite file with new data
				appendWriter.append("\n");
				for (i = 0; i < lineCount; i++){
					if (fileContent[i].length() > 0) {
						appendWriter.append(fileContent[i]);
					}
				}
			}

			appendWriter.close();
			bw.close();
			fw.close();
			bufferedReader.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		return chamberCount;
	}
}

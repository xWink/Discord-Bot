package DiscordBot.commands;

import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class Roulette {

	public static int roulette(User author, String path3, int chamberCount, MessageChannel channel){

		Random rand = new Random();
		File file = new File(path3);

		// Calculate whether the user died
		int pull = rand.nextInt(chamberCount);
		int boom;
		if (pull == 0){
			boom = 1;
			chamberCount = 6;
			channel.sendMessage("Bang! You died :skull:").queue();
		} else{
			boom = 0;
			chamberCount--;
			channel.sendMessage("Click. You survived  <:poggies:564285288621539328>").queue();
		}

		channel.sendMessage("Chambers left in the cylinder: ||"+chamberCount+"||").queue();

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
				fileContent[i] = line+"\n";
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
		}
		return chamberCount;
	}
}

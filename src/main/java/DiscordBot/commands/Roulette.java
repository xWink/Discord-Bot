package DiscordBot.commands;

import com.opencsv.CSVWriter;
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
			channel.sendMessage("Click! You survived. For now...").queue();
		}

		try {
			// Create writers, readers, boolean, etc
			FileWriter fileWriter = new FileWriter(file, true);
			CSVWriter csvWriter = new CSVWriter(fileWriter);
			Path filePath = Paths.get(path3);
			BufferedReader reader = Files.newBufferedReader(filePath);
			boolean found = false;
			String[] header = {"Name", "Attempts", "Deaths"};
			int attempts;
			int deaths;

			// If file is empty, give it appropriate headers
			if (reader.readLine() == null) {
				csvWriter.writeNext(header);
				csvWriter.close();
				fileWriter.close();
			}

			// Get number of lines
			BufferedReader bufferedReader = Files.newBufferedReader(filePath);
			int lineCount = 0;
			int i = 0;
			String line;
			while (bufferedReader.readLine() != null) {
				lineCount++;
			}
			String[] fileContent = new String[lineCount];

			// Store file content in array
			while ((line = reader.readLine()) != null) {
				fileContent[i] = line + "\n";
				i++;
			}
			reader.close();

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
					System.out.println(fileContent[i]);
					break;
				}
			}

			// If user not found, add new name to file
			if (!found){
				System.out.println("Not found");
				String[] newPlayer = {author.getId(), "1", Integer.toString(boom)};
				System.out.println("created string");
				csvWriter.writeNext(newPlayer, true);
				System.out.println("wrote");

				fileWriter.close();
				csvWriter.close();
			} else {
				// Erase file
				System.out.println("found");
				PrintWriter printWriter = new PrintWriter(file);
				printWriter.print("");
				printWriter.close();

				// Print header
				csvWriter.writeNext(header);
				csvWriter.close();
				fileWriter.close();

				for (i = 0; i < lineCount; i++){
					fileWriter.write(fileContent[i]);
				}
				System.out.println("wrote");
			}

			System.out.println("Finished");

		} catch (IOException e) {
			e.printStackTrace();
		}

		return chamberCount;
	}
}

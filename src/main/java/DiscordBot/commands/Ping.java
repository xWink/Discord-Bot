package DiscordBot.commands;

import com.opencsv.CSVWriter;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Ping {
	public static void ping(User author, Event event, MessageChannel channel, String path2){
		// Get high and low scores into memory
		String bestMem = "null", worstMem = "null", bestNum = "0", worstNum = "0";
		long best = 999, worst = 0;
		File scores = new File(path2);
		try {
			FileWriter fileWriter = new FileWriter(scores, true);
			CSVWriter csvWriter = new CSVWriter(fileWriter);
			Path scorePath = Paths.get(path2);
			BufferedReader reader = Files.newBufferedReader(scorePath);
			String line;

			// If file is empty, write 2 sets of name,score
			if ((line = reader.readLine()) == null) {
				String[] bestHead = {"Name", "999"};
				String[] worstHead = {"Name", "0"};

				csvWriter.writeNext(bestHead,true);
				csvWriter.writeNext(worstHead,true);
			}
			// If file has content, read it into memory
			else{
				bestMem = line.substring(line.indexOf("\"")+1,line.indexOf("\"",line.indexOf("\"")+1));
				bestNum = line.substring(line.lastIndexOf(",")+2,line.indexOf("\"", line.lastIndexOf(",")+2));
				best = Integer.parseInt(bestNum);

				line = reader.readLine();
				worstMem = line.substring(line.indexOf("\"")+1,line.indexOf("\"",line.indexOf("\"")+1));
				worstNum = line.substring(line.lastIndexOf(",")+2,line.indexOf("\"", line.lastIndexOf(",")+2));
				worst = Integer.parseInt(worstNum);
			}

			csvWriter.close();
			fileWriter.close();
			reader.close();
		} catch(IOException e){
			e.printStackTrace();
		}

		// Print message
		channel.sendMessage("Pong! " + event.getJDA().getPing() + " ms").queue();
		String authorString = author.toString().substring(author.toString().indexOf(":") + 1, author.toString().lastIndexOf("("));

		// If they get the best score, add it to the CSV file
		if (event.getJDA().getPing() < best) {
			channel.sendMessage("You got the best score!").queue();
			bestNum = Long.toString(event.getJDA().getPing());
			try {
				FileWriter fileWriter = new FileWriter(scores, false);
				CSVWriter csvWriter = new CSVWriter(fileWriter);

				String[] updateBest = {authorString, bestNum};
				System.out.println(authorString);
				csvWriter.writeNext(updateBest, true);
				String[] updateWorst = {worstMem, worstNum};
				csvWriter.writeNext(updateWorst, true);

				csvWriter.close();
				fileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// If they get the worst score, add it to the CSV file
		if (event.getJDA().getPing() > worst) {
			channel.sendMessage("Wow, you got the worst score! What brand toaster runs Discord?").queue();
			worstNum = Long.toString(event.getJDA().getPing());
			try {
				FileWriter fileWriter = new FileWriter(scores, false);
				CSVWriter csvWriter = new CSVWriter(fileWriter);

				String[] updateBest = {bestMem, bestNum};
				csvWriter.writeNext(updateBest);
				String[] updateWorst = {authorString, worstNum};
				csvWriter.writeNext(updateWorst);

				csvWriter.close();
				fileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

package DiscordBot.commands;

import com.opencsv.CSVWriter;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Scores {

	public static void scores(MessageChannel channel, String path2){

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
		channel.sendMessage("The best score is "+best+" ms from "+bestMem+"!").queue();
		channel.sendMessage("The worst score is "+worst+" ms from "+worstMem+"!").queue();
	}
}

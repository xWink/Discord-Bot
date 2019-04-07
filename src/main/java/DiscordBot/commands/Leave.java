package DiscordBot.commands;

import com.opencsv.CSVWriter;
import net.dv8tion.jda.core.entities.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Leave {
	public static void leave(Member auth, User author, MessageChannel channel, Guild guild, String content, String path){
		String roleName = content.substring(7);
		boolean eventHappened = false;
		try {
			Path filePath = Paths.get(path);
			// Get number of lines
			BufferedReader bufferedReader = Files.newBufferedReader(filePath);
			int lineCount = 0;
			int i = 0;
			String line;
			while(bufferedReader.readLine() != null){
				lineCount++;
			}
			String[] fileContent = new String[lineCount];

			// Store file content in array
			BufferedReader reader = Files.newBufferedReader(filePath);
			while((line = reader.readLine()) != null){
				fileContent[i] = line+"\n";
				i++;
			}

			// Find application and erase it from csv
			for (i = 0; i < lineCount; i++){
				if (fileContent[i].equals("\""+roleName+"\","+"\""+author.getId()+"\"\n")){
					fileContent[i] = "";
					eventHappened = true;
					channel.sendMessage("I've deleted your application for \""+roleName+"\"").queue();
				}
			}

			//Rewrite csv file
			File file = new File(path);
			FileWriter fileWriter = new FileWriter(file, false);
			CSVWriter csvWriter = new CSVWriter(fileWriter);
			for (i = 0; i < lineCount; i++) {
				fileWriter.write(fileContent[i]);
			}
			csvWriter.close();
			fileWriter.close();

			// Remove role from user
			try {
				guild.getController().removeSingleRoleFromMember(auth, guild.getRolesByName(roleName, true).get(0)).queue();
				channel.sendMessage(auth.getAsMention()+" left "+roleName).queue();
			}catch(IndexOutOfBoundsException e){
				if (!eventHappened) {
					channel.sendMessage("You do not have this role!").queue();
				}
			}

		}catch (IOException e){
			e.printStackTrace();
		}
	}
}

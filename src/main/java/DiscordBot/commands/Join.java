package DiscordBot.commands;

import com.opencsv.CSVWriter;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Join {
	public static void join(Member auth, User author, MessageChannel channel, Guild guild, String content, String path){
		String roleName = content.substring(6);
		// If role is restricted, don't assign user to role
		if (roleName.toLowerCase().equals("moderator") || roleName.toLowerCase().contains("verified")){
			channel.sendMessage("I cannot set you to that role").queue();
			return;
		}
		// If role exists and isn't restricted, assign user to role
		if (guild.getRolesByName(roleName,true).get(0).getName().toLowerCase().equals(roleName.toLowerCase())) {
			guild.getController().addRolesToMember(auth, guild.getRolesByName(roleName, true)).queue();
			channel.sendMessage("Role \""+roleName+"\" added to "+auth.getAsMention()).queue();
		}
		else{ // If role does not exist
System.out.println("Here1");
			File file = new File(path);
			try {
				// Create writers, readers, threshold, etc
				int threshold = 4; // Required number of applicants for new role
				boolean alreadyExists = false;
				FileWriter fileWriter = new FileWriter(file, true);
				CSVWriter csvWriter = new CSVWriter(fileWriter);
				Path filePath = Paths.get(path);
				BufferedReader reader = Files.newBufferedReader(filePath);

				// If file is empty, give it appropriate headers
				if (reader.readLine() == null){
System.out.println("Here2");
					String[] header = {"CourseID", "Applicant"};
					csvWriter.writeNext(header);
				}

				// If user already requested this role, don't add this application to file
				String line = reader.readLine();
				while (line != null){
					if (line.equals("\""+roleName+"\","+"\""+author.getId()+"\"")){
						alreadyExists = true;
					}
					line = reader.readLine();
				}

				// If this is a new application, add it to file
				if (!alreadyExists) {
System.out.println("Here3");
					String[] application = {roleName, author.getId()};
					csvWriter.writeNext(application, true);
				}
				reader.close();
				csvWriter.close();
				fileWriter.close();
System.out.println("Here4");

				// Check how many people applied for the same role
				String[] applicants = new String[threshold];
				int applicationCount = 0;
				BufferedReader reader2 = Files.newBufferedReader(filePath);
				line = reader2.readLine();
				while (line != null){
					if (line.startsWith("\""+roleName+"\",")){
System.out.println("Here5");
						applicants[applicationCount] = line.substring(line.indexOf("\"",roleName.length()+3)+1,line.length()-1);
						applicationCount++;
					}
					line = reader2.readLine();
				}
				reader2.close();

				// If number of applications is sufficient, create role and channel for it, and assign all applicants to that role
				if (applicationCount >= threshold && !alreadyExists){
					ArrayList<Permission> viewChannel = new ArrayList<>(); // Permissions for that channel
					viewChannel.add(0,Permission.VIEW_CHANNEL);

					guild.getController().createRole().setName(roleName).queue(); // Create the role
					guild.getController().createTextChannel(roleName).setParent(guild.getCategoriesByName("Electives",true).get(0)).complete(); // Create the textChannel
					TextChannel textChannel = guild.getTextChannelsByName(roleName,true).get(0); // Variable textChannel is the new channel

					// Give role to all applicants
					for (int i = 0; i < threshold; i++){
						guild.getController().addRolesToMember(guild.getMemberById(applicants[i]),guild.getRolesByName(roleName,true)).queue();
					}

					// Prevent everyone from seeing the channel
					textChannel.createPermissionOverride(guild.getRolesByName("@everyone",true).get(0)).setDeny(viewChannel).queue();
					// Let people with the specified role see the channel and read/send messages
					textChannel.createPermissionOverride(guild.getRolesByName(roleName,true).get(0)).setAllow(viewChannel).queue();
					textChannel.createPermissionOverride(guild.getRolesByName(roleName,true).get(0)).setAllow(Permission.MESSAGE_READ).queue();
					// Do not let people with this role do @everyone
					textChannel.createPermissionOverride(guild.getRolesByName(roleName,true).get(0)).setDeny(Permission.MESSAGE_MENTION_EVERYONE).queue();
					// Let moderators see the channel
					textChannel.createPermissionOverride(guild.getRolesByName("Moderator",true).get(0)).setAllow(viewChannel).queue();

					channel.sendMessage("The channel for your elective has been created! Only members of the channel can see it.").queue();
				}
				else{ // If number of applications is too low
					if (alreadyExists){
						channel.sendMessage("You already applied for this role!\nI need "+(threshold - applicationCount)+" more requests to make it").queue();
					}
					else{
						channel.sendMessage("Role \"" + roleName + "\" does not exist, but the request has been noted.\nI need "+(threshold - applicationCount)+" more requests to make it").queue();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

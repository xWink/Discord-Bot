/*
Copyright 2019 Shawn Kaplan

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
(the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

import com.opencsv.CSVWriter;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class MyEventListener extends ListenerAdapter {
	@Override
	public void onMessageReceived(MessageReceivedEvent event){

		User author = event.getAuthor(); // Variable author is the author of type User
		if (author.isBot()) return; // If the event is made by the bot, ignore it

		Message message = event.getMessage(); // Variable message is the detected message
		String content = message.getContentRaw(); // Variable content is the text of the message
		MessageChannel channel = event.getChannel(); // Variable channel is the text channel the message came from
		Guild guild = event.getGuild(); // Variable guild is the Discord server
		Member auth = guild.getMember(author); // Variable auth is author of type Member
		String path = "C:\\Users\\Nicholas\\Cloud\\Nextcloud\\Coding\\BCompBot\\ElectiveRequests.csv"; // applicant file path
		String path2 = "C:\\Users\\Nicholas\\Cloud\\Nextcloud\\Coding\\BCompBot\\ScoreList.csv"; // score file path

		// Get high and low scores into memory
		String bestMem = null, worstMem = null, bestNum, worstNum;
		int best = 999, worst = 0;
		File scores = new File(path2);
		try {
			Path scorePath = Paths.get(path2);
			BufferedReader reader = Files.newBufferedReader(scorePath);
			FileWriter fileWriter = new FileWriter(scores, true);
			CSVWriter csvWriter = new CSVWriter(fileWriter);
			String line;

			// If file is empty, write 2 sets of name,score
			if ((line = reader.readLine()) == null) {
				String[] bestHead = {"Name", "999"};
				String[] worstHead = {"Name", "0"};

				csvWriter.writeNext(bestHead);
				csvWriter.writeNext(worstHead);
			}
			// If file has content, read it into memory
			else{
				bestMem = line.substring(0,line.indexOf(",")-1);
				bestNum = line.substring(line.indexOf(",")+1);
				best = Integer.parseInt(bestNum);

				line = reader.readLine();
				worstMem = line.substring(0,line.indexOf(",")-1);
				worstNum = line.substring(line.indexOf(",")+1);
				worst = Integer.parseInt(worstNum);
			}
		} catch(IOException e){
			e.printStackTrace();
		}


		// Bot shows how to use !join
		if (content.toLowerCase().equals("!join") || content.toLowerCase().equals("!join ")){
			channel.sendMessage("Command: !join <courseID>\n\nExample: !join mcs2100").queue();
		}

		// Bot shows how to use !leave
		else if (content.toLowerCase().equals("!leave") || content.toLowerCase().equals("!leave ")){
			channel.sendMessage("Command: !leave <courseID>\n\nExample: !leave mcs2100").queue();
		}

		// Bot shows how to use !join and !leave
		else if (content.toLowerCase().equals("!help")){
			channel.sendMessage("For instructions to join an elective group, say \"!join\" and to leave one, say \"!leave\"").queue();
		}

		// Bot responds with pong and latency
		else if (content.toLowerCase().equals("!ping")) {
			channel.sendMessage("Pong! " + event.getJDA().getPing() + " ms").queue();
			if (event.getJDA().getPing() > worst){
				channel.sendMessage("You beat the high score!").queue();
			}
		}

		// Bot creates new text channel and deletes old one (OWNER ONLY)
		else if (content.toLowerCase().equals("!totalchatwipe")) {
			if (auth.equals(guild.getOwner())) {
				event.getGuild().getTextChannelById(event.getChannel().getId()).createCopy().queue();
				event.getGuild().getTextChannelById(event.getChannel().getId()).delete().queue();
			}
			else{
				channel.sendMessage("You do not have permission to do that!").queue();
			}
		}

		// Bot gives requested role to target (MODERATOR->PEASANT ONLY)
		else if(content.toLowerCase().startsWith("!giverole ")){
			try {
				content = content.substring(10);
				String roleName = content.substring(content.indexOf(" ") + 1);
				Member member = message.getMentionedMembers().get(0);
				// If author is a moderator and target is not a moderator
				if (auth.getRoles().containsAll(guild.getRolesByName("Moderator", true))
						&& !member.getRoles().containsAll(guild.getRolesByName("Moderator", true))) {
					guild.getController().addRolesToMember(member, guild.getRolesByName(roleName, true)).queue();
				} else {
					channel.sendMessage("You do not have permission to do that!").queue();
				}
			} catch(IndexOutOfBoundsException e){
				channel.sendMessage("Command: !giverole <@user> <role>").queue();
			}
		}

		// Bot removes requested role from user (MODERATOR->PEASANT ONLY)
		else if(content.startsWith("!takerole ")) {
			try {
				content = content.substring(10);
				String roleName = content.substring(content.indexOf(" ") + 1);
				Member member = message.getMentionedMembers().get(0);
				// If author is a moderator and target is not a moderator
				if (auth.getRoles().containsAll(guild.getRolesByName("Moderator", true))
						&& !member.getRoles().containsAll(guild.getRolesByName("Moderator", true))) {
					guild.getController().removeRolesFromMember(member, guild.getRolesByName(roleName, true)).queue();
				} else {
					channel.sendMessage("You do not have permission to do that!").queue();
				}
			}catch (IndexOutOfBoundsException e){
				channel.sendMessage("Command: !takerole <@user> <role>").queue();
			}
		}

		// User requests to join/create an elective role (EVERYONE)
		else if(content.startsWith("!join ")){
			String roleName = content.substring(6);
			// If role is restricted, don't assign user to role
			if (roleName.toLowerCase().equals("moderator") || roleName.toLowerCase().contains("verified")){
				channel.sendMessage("I cannot set you to that role").queue();
				return;
			}
			// If role exists and isn't restricted, assign user to role
			if (!guild.getRolesByName(roleName,true).equals(guild.getRolesByName("lamptissueboxfritoscoke",true))) {
				guild.getController().addRolesToMember(auth, guild.getRolesByName(roleName, true)).queue();
				channel.sendMessage("Role \""+roleName+"\" added to "+auth.getAsMention()).queue();
			}
			else{ // If role does not exist
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
						String[] application = {roleName, author.getId()};
						csvWriter.writeNext(application, true);
					}
					reader.close();
					csvWriter.close();
					fileWriter.close();
					// Check how many people applied for the same role
					String[] applicants = new String[threshold];
					int applicationCount = 0;
					BufferedReader reader2 = Files.newBufferedReader(filePath);
					line = reader2.readLine();
					while (line != null){
						if (line.startsWith("\""+roleName+"\",")){
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
						// Let people with the specified role see the channel
						textChannel.createPermissionOverride(guild.getRolesByName(roleName,true).get(0)).setAllow(viewChannel).queue();
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

		// Remove user's application from CSV file
		else if (content.toLowerCase().startsWith("!leave ")){
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

		// Delete all non-specified roles (OWNER ONLY)
		else if (content.toLowerCase().equals("!cleanroles")){
			if (auth.isOwner()){
				List <Role> listRoles = guild.getRoles();
				for (Role listRole : listRoles) { // Delete all roles that are not these
					String substring = listRole.toString().toLowerCase().substring(2, listRole.toString().lastIndexOf("("));
					if (!(substring.equals("moderator") || substring.equals("verified students") || substring.equals("@everyone") || substring.equals("discordbot"))) {
						listRole.delete().queue();
					}
				}
				channel.sendMessage("All non-essential roles deleted!").queue();
			}
			else{
				channel.sendMessage("You do not have permission to do that!").queue();
			}
		}

		// Delete all elective channels (OWNER ONLY)
		else if(content.toLowerCase().equals("!cleanelectives")){
			if (auth.isOwner()){
				List <Channel> listChannels = guild.getChannels();
				for (Channel listChannel : listChannels){
					if (listChannel.getParent() != null && listChannel.getParent().equals(guild.getCategoriesByName("Electives",true).get(0))){
						listChannel.delete().queue();
					}
				}
				channel.sendMessage("All elective channels deleted!").queue();
			}
			else{
				channel.sendMessage("You do not have permission to do that!").queue();
			}
		}

		// Show score
		else if (content.toLowerCase().equals("!score")){
			channel.sendMessage("The best score is "+best+" from "+best+"!").queue();
		}
	}
}
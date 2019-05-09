package DiscordBot.commands;

import DiscordBot.RoleBot;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

import java.sql.*;
import java.util.ArrayList;

public class Join {

	public static void join(Member auth, User author, MessageChannel channel, Guild guild, String content){

		String roleName = content.substring(6);
		// If role is restricted, don't assign user to role
		if (roleName.toLowerCase().equals("moderator") || roleName.toLowerCase().contains("verified")){
			channel.sendMessage("I cannot set you to that role").queue();
			return;
		}
		// If role exists and isn't restricted, assign user to role
		if (!guild.getRolesByName(roleName,true).isEmpty()) {
			if (guild.getMember(author).getRoles().contains((guild.getRolesByName(roleName, true).get(0)))){
				System.out.println("Success");
			}
			guild.getController().addRolesToMember(auth, guild.getRolesByName(roleName, true)).queue();
			channel.sendMessage("Role \""+roleName+"\" added to "+auth.getAsMention()).queue();
		}
		// If role does not exist
		else{
			Connection conn;
			ResultSet rs;

			try {
				// Connect to database
				Class.forName("org.mariadb.jdbc.Driver");
				conn = DriverManager.getConnection("jdbc:mysql://localhost/discord_bot", RoleBot.config.db_user, RoleBot.config.db_pass);

				// Look for the role application in the database
				PreparedStatement findRole = conn.prepareStatement("SELECT * FROM roles WHERE name = '"+roleName+"'");
				rs = findRole.executeQuery();
			}
			catch (Exception e){
				System.out.println("Join Exception 1");
				System.out.println("Exception: "+ e.toString());
				System.out.println("Failed to connect to database, terminating command");
				channel.sendMessage("Encountered an error. Please inform an admin :(").queue();
				return;
			}

			// If role application doesn't exist, add it
			try {
				if (!rs.isBeforeFirst()){
					PreparedStatement addRole = conn.prepareStatement("INSERT INTO roles VALUES ('"+roleName+"', "+author.getIdLong()+", null, null)");
					addRole.executeUpdate();
					channel.sendMessage("Your role application was added to the server!").queue();
					return;
				}
			}
			catch (SQLException e){
				System.out.println("Join Exception 2");
				System.out.println("Exception: "+e.toString());
				channel.sendMessage("Encountered an error. Please inform an admin :(").queue();
			}

			// If application does exist, check if the applicant has already applied
			try {
				rs.first();
				if (rs.getFloat("user1") == author.getIdLong() ||
						rs.getFloat("user2") == author.getIdLong() ||
						rs.getFloat("user3") == author.getIdLong()){
					channel.sendMessage("You have already applied for this role!").queue();
					return;
				}
			}
			catch (SQLException e){
				System.out.println("Join Exception 3");
				System.out.println("Exception: "+e.toString());
				channel.sendMessage("Encountered an error. Please inform an admin :(").queue();
			}

			// If they haven't applied, check how many applicants there are
			try {
				rs.first();
				// If the number of applicants is not full, add the applicant
				if (rs.getLong("user2") == 0){
					PreparedStatement apply = conn.prepareStatement("UPDATE roles SET user2 = "+author.getIdLong()+" WHERE name = '"+roleName+"'");
					apply.executeUpdate();
					channel.sendMessage("You are applicant #2 for this role!").queue();
					return;
				}
				if (rs.getLong("user3") == 0){
					PreparedStatement apply = conn.prepareStatement("UPDATE roles SET user3 = "+author.getIdLong()+" WHERE name = '"+roleName+"'");
					apply.executeUpdate();
					channel.sendMessage("You are applicant #3 for this role!").queue();
					return;
				}
			}
			catch (SQLException e){
				System.out.println("Join Exception 4");
				System.out.println("Exception: "+e.toString());
				channel.sendMessage("Encountered an error. Please inform an admin :(").queue();
			}

			// If the number of applicants is full, create the role and channel and assign previous applicants to them
			ArrayList<Permission> viewChannel = new ArrayList<>(); // Permissions for that channel
			viewChannel.add(0,Permission.VIEW_CHANNEL);
			double applicants[] = new double[4];
			applicants[0] = author.getIdLong();

			try {
				for (int i = 1; i < 4; i++) {
					applicants[i] = rs.getDouble("user"+i);
					if (i < 3)
						rs.next();
				}
			}
			catch(SQLException e){
				System.out.println("Join Exception 5");
				System.out.println("Exception: "+e.toString());
				channel.sendMessage("Encountered an error. Please inform an admin :(").queue();
			}

			guild.getController().createRole().setName(roleName).queue(); // Create the role
			guild.getController().createTextChannel(roleName).setParent(guild.getCategoriesByName("Electives",true).get(0)).complete(); // Create the textChannel
			TextChannel textChannel = guild.getTextChannelsByName(roleName,true).get(0); // Variable textChannel is the new channel

			// Give role to all applicants
			for (int i = 0; i < 4; i++){
				guild.getController().addRolesToMember(guild.getMemberById((long)applicants[i]),guild.getRolesByName(roleName,true)).queue();
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
	}
}

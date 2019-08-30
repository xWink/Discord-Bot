package DiscordBot.commands.groups;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

import java.sql.*;

public class Join {

	private static boolean roleExists(Guild guild, User author, Member auth, TextChannel channel, String roleName){

		// If role exists
		if (!guild.getRolesByName(roleName,true).isEmpty()) {
			// If user already has the role, tell them
			if (guild.getMember(author).getRoles().contains((guild.getRolesByName(roleName, true).get(0)))) {
				channel.sendMessage("You already have this role!").complete();
			}
			// If role is restricted, don't assign user to role
			else if (guild.getTextChannelsByName(roleName,true).isEmpty() || !guild.getJDA().getCategoryById("556266020625711130").getTextChannels().contains(guild.getTextChannelsByName(roleName, true).get(0))){
				channel.sendMessage("I cannot set you to that role").complete();
			}
			// If user doesn't have the role and it is not restricted, give it to them
			else {
				guild.getController().addRolesToMember(auth, guild.getRolesByName(roleName, true)).queue();
				channel.sendMessage("Role \"" + roleName + "\" added to " + auth.getAsMention()).complete();
			}
			return true;
		}

		return false;
	}

	public static void join(Member auth, User author, TextChannel channel, Guild guild, String content, Connection conn){

		// If format is incorrect, show user how to properly format command
		if (content.equalsIgnoreCase("!join") || content.equalsIgnoreCase("!join ")){
			channel.sendMessage("Command: !join <courseID>\n`Example: !join mcs2100`").queue();
			return;
		}

		String roleName = content.substring(6); // Get roleName

		// Return if role exists
		if (roleExists(guild, author, auth, channel, roleName))
			return;

		// Ensure new role application is in course ID format
		if (!content.substring(5).matches("^ (([A-Z][A-Z][A-Z][A-Z]?)|([a-z][a-z][a-z][a-z]?))[0-9][0-9][0-9][0-9]$")){
			channel.sendMessage("You can only apply to join roles for a course ID\nExample: !join biol2400").queue();
			return;
		}

		// Look for the role application in the database
		ResultSet rs;
		try {
			PreparedStatement findRole = conn.prepareStatement("SELECT * FROM roles WHERE name = '"+roleName+"'");
			rs = findRole.executeQuery();

			// If role application doesn't exist, add it
			if (!rs.isBeforeFirst()){
				PreparedStatement addRole = conn.prepareStatement("INSERT INTO roles VALUES ('"+roleName+"', "+author.getIdLong()+", null, null)");
				addRole.executeUpdate();
				channel.sendMessage("Your role application was added to the server!").complete();
				return;
			}

			// If application does exist, check if the applicant has already applied
			rs.first();
			if (rs.getFloat("user1") == author.getIdLong() ||
					rs.getFloat("user2") == author.getIdLong() ||
					rs.getFloat("user3") == author.getIdLong()){
				channel.sendMessage("You have already applied for this role!").complete();
				return;
			}

			// If they haven't applied, check how many applicants there are
			boolean applied = false;
			int applicationCount = 1;
			rs.first();

			// If the number of applicants is not full, add the applicant
			if (rs.getLong("user1") == 0){
				PreparedStatement apply = conn.prepareStatement("UPDATE roles SET user1 = "+author.getIdLong()+" WHERE name = '"+roleName+"'");
				apply.executeUpdate();
				applied = true;
			}
			else if (rs.getLong("user2") == 0){
				PreparedStatement apply = conn.prepareStatement("UPDATE roles SET user2 = "+author.getIdLong()+" WHERE name = '"+roleName+"'");
				apply.executeUpdate();
				applied = true;
			}
			else if (rs.getLong("user3") == 0){
				PreparedStatement apply = conn.prepareStatement("UPDATE roles SET user3 = "+author.getIdLong()+" WHERE name = '"+roleName+"'");
				apply.executeUpdate();
				applied = true;
			}

			if (applied){
				// Count number of applicants
				for (int i = 1; i < 4; i++){
					if (rs.getLong("user"+i) != 0)
						applicationCount++;
				}
				// Send message response
				channel.sendMessage("You are applicant #"+applicationCount+" for this role!").complete();
				return;
			}

			// If the number of applicants is full, create the role and channel and assign previous applicants to them
			long[] applicants = new long[4];
			applicants[0] = author.getIdLong();

			rs.first();
			for (int i = 1; i < 4; i++)
				applicants[i] = rs.getLong("user"+i);

			// Create role and channel
			guild.getController().createRole().setName(roleName).queue(); // Create the role
			guild.getController().createTextChannel(roleName).setParent(guild.getCategoriesByName("Electives",true).get(0)).complete(); // Create the textChannel
			TextChannel textChannel = guild.getTextChannelsByName(roleName,true).get(0); // Variable textChannel is the new channel
			Role role = guild.getRolesByName(roleName,true).get(0);

			// Give role to all applicants
			for (int i = 0; i < 4; i++)
				guild.getController().addRolesToMember(guild.getMemberById(applicants[i]), role).queue();

			// Set new channel permissions
			if (textChannel.getPermissionOverride(role) == null)
				textChannel.createPermissionOverride(role).complete();

			// Let people with the specified role see the channel and read/send messages
			textChannel.getPermissionOverride(role).getManager().grant(Permission.VIEW_CHANNEL).queue();
			textChannel.getPermissionOverride(role).getManager().grant(Permission.MESSAGE_READ).queue();

			// Prevent everyone from seeing the channel
			if (textChannel.getPermissionOverride(guild.getRolesByName("@everyone", true).get(0)) == null)
				textChannel.createPermissionOverride(guild.getRolesByName("@everyone", true).get(0)).complete();

			textChannel.getPermissionOverride(guild.getRolesByName("@everyone", true).get(0)).getManager().deny(Permission.MESSAGE_READ).queue();

			// Do not let people with this role do @everyone
			textChannel.getPermissionOverride(role).getManager().deny(Permission.MESSAGE_MENTION_EVERYONE).queue();

			channel.sendMessage("The channel you applied for was created! Only members of the channel can see it.").queue();
		}
		catch (Exception e){
			e.printStackTrace();
			channel.sendMessage("Encountered an error. Please inform an admin :(").complete();
		}
	}
}

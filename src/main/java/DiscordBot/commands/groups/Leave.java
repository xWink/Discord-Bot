package DiscordBot.commands.groups;

import net.dv8tion.jda.core.entities.*;

import java.sql.*;

public class Leave {

	public static void leave(Member auth, User author, MessageChannel channel, Guild guild, String content, Connection conn){

		String roleName = content.substring(7);
		ResultSet rs;
		Boolean removed = false;

		// If user has role, remove it
		if (!guild.getRolesByName(roleName,true).isEmpty() && auth.getRoles().contains(guild.getRolesByName(roleName,true).get(0))){
			guild.getController().removeSingleRoleFromMember(auth,guild.getRolesByName(roleName,true).get(0)).queue();
			channel.sendMessage("Removed "+roleName+" from "+auth.getAsMention()).queue();
			return;
		}

		try {
			// Look for the role application in the database
			PreparedStatement findRole = conn.prepareStatement("SELECT * FROM roles WHERE name = '"+roleName+"'");
			rs = findRole.executeQuery();
		}
		catch (Exception e){
			System.out.println("Leave Exception 1\nException: "+e.toString());
			channel.sendMessage("Encountered an error. Please inform an admin :(").complete();
			return;
		}

		// If role application exists and user applied for it, remove the application
		try{
			if (rs.next()){
				if (rs.getFloat("user1") == author.getIdLong()){
					PreparedStatement removeApplication = conn.prepareStatement("UPDATE roles SET user1 = null WHERE name = '"+roleName+"'");
					removeApplication.executeUpdate();
					removed = true;
				}
				if (rs.getFloat("user2") == author.getIdLong()){
					PreparedStatement removeApplication = conn.prepareStatement("UPDATE roles SET user2 = null WHERE name = '"+roleName+"'");
					removeApplication.executeUpdate();
					removed = true;
				}
				if (rs.getFloat("user3") == author.getIdLong()){
					PreparedStatement removeApplication = conn.prepareStatement("UPDATE roles SET user3 = null WHERE name = '"+roleName+"'");
					removeApplication.executeUpdate();
					removed = true;
				}
			}

			if (removed){
				channel.sendMessage("Your application for "+roleName+" has been removed").queue();
				return;
			}
		}
		catch (SQLException e){
			System.out.println("Leave Exception 1\nException: "+e.toString());
			channel.sendMessage("Encountered an error. Please inform an admin :(").complete();
		}

		// Tell them they didn't apply for it
		channel.sendMessage("You don't have an application for this role!").queue();
	}
}

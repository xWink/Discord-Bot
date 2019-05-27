package DiscordBot.commands.AdminCommands;

import net.dv8tion.jda.core.entities.*;

import java.util.List;

public class CleanRoles {

	public static void cleanRoles(Member auth, MessageChannel channel, Guild guild) {

		if (auth.isOwner()) {
			List<Role> listRoles = guild.getRoles();
			for (Role listRole : listRoles) { // Delete all roles that are not these
				String substring = listRole.toString().toLowerCase().substring(2, listRole.toString().lastIndexOf("("));
				if (!(substring.equals("moderator") || substring.equals("verified students") || substring.equals("@everyone") || substring.equals("discordbot"))) {
					listRole.delete().queue();
				}
			}
			channel.sendMessage("All non-essential roles deleted!").queue();
		} else {
			channel.sendMessage("You do not have permission to do that!").queue();
		}
	}
}

package DiscordBot.commands.admin_commands;

import net.dv8tion.jda.core.entities.*;

public class TakeRole {
	public static void takeRole(Member auth, TextChannel channel, Guild guild, String content, Message message){
		try {
			content = content.substring(10);
			String roleName = content.substring(content.indexOf(" ") + 1);
			Member member = message.getMentionedMembers().get(0);

			// If author is a moderator and target is not a moderator
			if (auth.getRoles().containsAll(guild.getRolesByName("Moderator", true))
					&& !member.getRoles().containsAll(guild.getRolesByName("Moderator", true))) {
				if (!guild.getRolesByName(roleName, true).isEmpty()) {
					guild.getController().removeSingleRoleFromMember(member, guild.getRolesByName(roleName, true).get(0)).queue();
					channel.sendMessage("Removed " + roleName + " role from <@" + member.getUser().getId() + ">").queue();
				}
				else{
					channel.sendMessage("That role does not exist").queue();
				}
			}
			else {
				channel.sendMessage("You do not have permission to do that!").queue();
			}
		}catch (IndexOutOfBoundsException e){
			channel.sendMessage("Command: !takerole <@user> <role>").queue();
		}
	}
}

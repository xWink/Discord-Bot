package DiscordBot.commands;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;

public class GiveRole {
	public static void giveRole(Member auth, MessageChannel channel, Guild guild, String content, Message message){
		try {
			content = content.substring(10);
			String roleName = content.substring(content.indexOf(" ") + 1);
			Member member = message.getMentionedMembers().get(0);
			// If author is a moderator and target is not a moderator
			if (auth.getRoles().containsAll(guild.getRolesByName("Moderator", true))
					&& !member.getRoles().containsAll(guild.getRolesByName("Moderator", true))) {
				guild.getController().addRolesToMember(member, guild.getRolesByName(roleName, true)).queue();
				channel.sendMessage("Added "+roleName+" role to "+member).queue();
			} else {
				channel.sendMessage("You do not have permission to do that!").queue();
			}
		} catch(IndexOutOfBoundsException e){
			channel.sendMessage("Command: !giverole <@user> <role>").queue();
		}
	}

}

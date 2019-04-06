package DiscordBot.commands;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;


public class TotalChatWipe {
	public static void chatWipe(Member auth, Guild guild, MessageChannel channel){
		if (auth.equals(guild.getOwner())) {
			guild.getTextChannelById(channel.getId()).createCopy().queue();
			guild.getTextChannelById(channel.getId()).delete().queue();
		}
		else{
			channel.sendMessage("You do not have permission to do that!").queue();
		}
	}
}

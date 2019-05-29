package DiscordBot.commands.admin_commands;

import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.List;

public class CleanElectives {
	public static void cleanElectives(Member auth, MessageChannel channel, Guild guild){
		if (auth.isOwner()){
			List<Channel> listChannels = guild.getChannels();
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
}

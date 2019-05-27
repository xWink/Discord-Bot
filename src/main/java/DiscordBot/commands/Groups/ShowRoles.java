package DiscordBot.commands.Groups;

import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.List;

public class ShowRoles {

    public static void showRoles(Guild guild, MessageChannel msgChannel){

        // Get all channels
        List<Channel> channelList = guild.getChannels();
        int channelCount = 0;

        // Find any channel in Electives category
        for (Channel channel:channelList) {
            if (channel.getParent() == guild.getCategoriesByName("electives", true).get(0)){
                if (channelCount == 0){
                    msgChannel.sendMessage("**Available elective roles**").queue();
                }
                // Print elective name
                channelCount++;
                msgChannel.sendMessage(channelCount+". "+channel.toString().substring(channel.toString().indexOf(":")+1,channel.toString().lastIndexOf("("))).queue();
            }
        }

        // If no channels found
        if (channelCount == 0){
            msgChannel.sendMessage("No available channels were found. You can apply to make new ones with `!join`").queue();
        }
    }
}

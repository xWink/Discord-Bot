package DiscordBot.commands.groups;

import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;

public class ShowRoles {

    public static void showRoles(Guild guild, TextChannel msgChannel){

        // Get all channels
        List<Channel> channelList = guild.getChannels();
        int channelCount = 0;
        String message = "**Available elective roles**";

        // Find any channel in Electives category
        for (Channel channel:channelList) {
            if (channel.getParent() == guild.getCategoriesByName("electives", true).get(0)){
                // Print elective name
                channelCount++;
                message = message.concat(channelCount+". "+channel.toString().substring(channel.toString().indexOf(":")+1,channel.toString().lastIndexOf("(")) + "\n");
            }
        }

        // If no channels found
        if (channelCount == 0){
            message = "No available channels were found. You can apply to make new ones with `!join`";
        }

        msgChannel.sendMessage(message).complete();
    }
}

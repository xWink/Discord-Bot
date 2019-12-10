package command.commands.misc;

import command.Command;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.List;

public class Roles extends Command {

    /**
     * Initializes the command's key to "!roles".
     */
    public Roles() {
        super("!roles", false);
    }

    /**
     * Shows available roles for joining.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        try {
            List<Channel> channelList = event.getGuild().getChannels();
            int channelCount = 0;
            String message = "**Available elective roles**\n";

            // Find any channel in Electives category
            for (Channel channel : channelList) {
                if (channel.getParent() == event.getGuild().getCategoriesByName("electives", true).get(0)) {
                    channelCount++;
                    System.out.println(channel.getName());
                    System.out.println(channel.toString());
                    message = message.concat(channelCount + ". " + channel.toString().substring(channel.toString().indexOf(":") + 1, channel.toString().lastIndexOf("(")) + "\n");
                }
            }

            // If no channels found
            if (channelCount == 0) {
                message = "No available channels were found. You can apply to make new ones with `!join`";
            }

            event.getChannel().sendMessage(message).queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

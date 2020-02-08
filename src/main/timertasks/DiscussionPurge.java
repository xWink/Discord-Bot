package main.timertasks;

import main.Server;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;

public class DiscussionPurge implements Runnable {

    /**
     * Purges all messages in the Discussion channel.
     */
    @Override
    public void run() {
        try {
            List<Message> messages;
            int size = 0;
            do {
                Guild guild = Server.getApi().getGuildById(Server.getGuild());
                if (guild == null)
                    return;
                TextChannel discussionChannel = guild.getTextChannelById(670857670214942730L);
                if (discussionChannel == null) {
                    System.out.println("NULL Discussion Channel!!!");
                    break;
                }
                messages = new MessageHistory(discussionChannel).retrievePast(99).complete();
                if (messages.size() == size)
                    break;
                size = messages.size();
                if (messages.size() > 22)
                    discussionChannel.deleteMessages(messages.subList(20, messages.size())).queue();
            } while (messages.size() > 22);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

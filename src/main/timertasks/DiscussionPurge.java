package main.timertasks;

import main.Server;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;

public class DiscussionPurge implements Runnable {

    /**
     * Purges all messages in the Discussion channel.
     */
    @Override
    public void run() {
        List<Message> messages;
        TextChannel discussionChannel = Server.getGuild().getTextChannelById(670857670214942730L);
        MessageHistory history = new MessageHistory(discussionChannel);

        try {
            do {
                messages = history.retrievePast(100).complete();
                discussionChannel.deleteMessages(messages.subList(20, messages.size())).queue();
            } while (messages.size() > 20);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

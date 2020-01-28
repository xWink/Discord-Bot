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
        TextChannel discussionChannel = Server.getGuild().getTextChannelById(652635445179711496L);
        MessageHistory history = new MessageHistory(discussionChannel);

        try {
            List<Message> messages = history.retrievePast(history.size()).complete();
            if (messages.size() > 50) {
                discussionChannel.deleteMessages(messages.subList(50, messages.size())).queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

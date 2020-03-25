package main.timertasks;

import main.Server;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;

public class DiscussionPurge implements Runnable {

    /**
     * Purges all messages in the Discussion channel up to the last 50. Not functional due to rate limiter.
     */
    @Override public void run() {
        Guild guild = Server.getApi().getGuildById(Server.GUILD_ID);
        if (guild == null) {
            return;
        }

        MessageChannel channel = guild.getTextChannelById(670857670214942730L);
        if (channel == null) {
            return;
        }

        purgeMessages(channel);
    }

    private void purgeMessages(MessageChannel channel) {
        channel.getHistory().retrievePast(100).queue(messages -> {
            if (messages.size() > 50) {
                channel.purgeMessages(messages.subList(50, messages.size()));
                purgeMessages(channel);
            }
        });
    }
}

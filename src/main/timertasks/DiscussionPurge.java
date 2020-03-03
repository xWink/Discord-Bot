package main.timertasks;

import main.Server;
import net.dv8tion.jda.api.entities.*;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class DiscussionPurge implements Runnable {

    /**
     * Purges all messages in the Discussion channel up to the last 50.
     */
    @Override public void run() {
        Guild guild = Server.getApi().getGuildById(Server.getGuild());
        if (guild == null) {
            return;
        }

        MessageChannel channel = guild.getTextChannelById(670857670214942730L);
        if (channel == null) {
            return;
        }

        final int numMessages = 50;
        MessageHistory history = channel.getHistory();

        while (history.size() > numMessages) {
            history.retrievePast(Math.min(100, history.size())).queue(messages ->
                    channel.purgeMessages(messages.subList(numMessages, Math.min(messages.size(), 100))));
        }
    }
}

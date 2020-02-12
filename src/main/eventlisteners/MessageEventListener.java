package main.eventlisteners;

import command.Command;
import command.CommandList;
import command.util.message.MessageData;
import database.connectors.MessagesConnector;
import main.Config;
import main.Server;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MessageEventListener extends ListenerAdapter {

    MessagesConnector mc = new MessagesConnector();

    /**
     * Upon receiving a message, the bot checks if the message meets the
     * criteria to be a command, then compares the message to every command's
     * key. If a message matches with a command key, the command is run.
     *
     * @param event the MessageReceivedEvent potentially containing a command key
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String messageContent = event.getMessage().getContentRaw();
        if (!event.getAuthor().isBot()) {
            if (messageContent.startsWith("!")) {
                for (Command command : CommandList.getCommands()) {
                    if (command.keyMatches(messageContent)
                            && (event.getChannel().getId().equals(Config.getChannels()[0]) || command.isGlobal())) {
                        command.start(event);
                    }
                }
            }

            try {
                mc.storeMessage(event.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        try {
            MessageData data = mc.getMessageDataById(event.getMessageIdLong());
            if (data != null)
                Objects.requireNonNull(Server.getApi().getTextChannelById(677109914400980992L))
                        .sendMessage(data.toFormattedString()).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package main.eventlisteners;

import command.Command;
import main.CommandList;
import command.util.misc.MessageData;
import database.connectors.MessagesConnector;
import main.Server;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageReceivedEventListener extends ListenerAdapter {

    private MessagesConnector mc = new MessagesConnector();

    /**
     * Upon receiving a message, the bot checks if the message meets the
     * criteria to be a command, then compares the message to every command's
     * key. If a message matches with a command key, the command is run.
     * Messages that do not trigger bot commands are stored in the database
     *
     * @see MessageData
     * @see MessagesConnector
     * @param event the MessageReceivedEvent potentially containing a command key
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String messageContent = event.getMessage().getContentRaw();
        if (!event.getAuthor().isBot()) {
            if (messageContent.startsWith("!")) {
                // Find matching command
                for (Command command : CommandList.getCommands()) {
                    if (command.keyMatches(messageContent)
                            && (event.getChannel().getIdLong() == (Server.BOTS_CHANNEL_ID) || command.isGlobal())) {
                        command.start(event);
                        return;
                    }
                }
            }

            try {
                mc.storeMessage(event.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (event.getChannel().getIdLong() == Server.CHANNELS_CHANNEL_ID) {
            Emote checkMark = event.getGuild().getEmoteById(Server.CHECK_EMOJI_ID);
            if (checkMark != null && messageNeedsCheckMark(messageContent))
                event.getMessage().addReaction(checkMark).queue();
        }
    }

    private boolean messageNeedsCheckMark(String message) {
        return !message.startsWith("-") && !message.startsWith("<@") && !message.startsWith("**");
    }
}

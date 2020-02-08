package main.eventlisteners;

import command.Command;
import command.CommandList;
import main.Config;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageEventListener extends ListenerAdapter {

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
        if (!event.getAuthor().isBot() && messageContent.startsWith("!")) {
            for (Command command : CommandList.getCommands()) {
                if (command.keyMatches(messageContent)) {
                    if (event.getChannel().getId().equals(Config.getChannels()[0]) || command.isGlobal()) {
                        command.start(event);
                    }
                }
            }
        }
    }
}

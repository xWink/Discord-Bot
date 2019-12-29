package command.commands.admin;

import command.AdminCommand;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.List;

public class Purge extends AdminCommand {

    /**
     * Initializes the command's key to "!purge" and is global.
     */
    public Purge() {
        super("!purge", true);
    }

    /**
     * Compares a string to the command's key and checks if that
     * string starts with the key.
     *
     * @param string the user's input being compared to the key
     * @return returns true if the key matches and false otherwise
     */
    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey());
    }

    /**
     * Deletes a given number of messages in a text channel (not including the purge command message).
     * Or deletes all messages by a mentioned user up to a specified number of messages back in the channel
     * history.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    protected void runCommand(MessageReceivedEvent event) {
        if (!isAdmin(event.getMember())) {
            return;
        }

        String[] strings = event.getMessage().getContentRaw().split(" ");
        MessageHistory history = new MessageHistory(event.getTextChannel());

        try {
            int numMessages = Integer.parseInt(strings[1]);
            List<Message> messages = history.retrievePast(numMessages + 1).complete();
            event.getTextChannel().deleteMessages(messages).queue();
        } catch (Exception e) {
            List<Message> messages = history.retrievePast(1).complete();
            event.getTextChannel().deleteMessages(messages).queue();
        }
    }
}

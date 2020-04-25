package command.commands.admin;

import command.AdminCommand;
import command.Command;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Purge extends Command implements AdminCommand {

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
     * Deletes a given number of messages in a text channel (not including the purge command message) up to 99.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        if (!AdminCommand.memberIsAdmin(event.getMember())) {
            return;
        }

        String[] strings = event.getMessage().getContentRaw().split(" ");
        MessageHistory history = new MessageHistory(event.getTextChannel());

        try {
            int numMessages = Integer.parseInt(strings[1]);
            if (numMessages < 100) {
                history.retrievePast(numMessages + 1)
                        .queue(messages -> event.getChannel().purgeMessages(messages));
            } else {
                history.retrievePast(1).queue(messages -> event.getChannel().purgeMessages(messages));
            }
        } catch (Exception e) {
            history.retrievePast(1).queue(messages -> event.getChannel().purgeMessages(messages));
        }
    }
}

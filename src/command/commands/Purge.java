package command.commands;

import command.Command;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Purge extends Command {

    /**
     * Initializes the command's key to "!purge".
     */
    public Purge() {
        super("!purge");
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
        return string.startsWith(getKey());
    }

    /**
     * Deletes a given number of messages in a text channel.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        if (!event.getMember().canInteract(event.getGuild().getRoleById("486635066928136194"))) {
            return;
        }

        try {
            int numMessages = Integer.parseInt(event.getMessage().getContentRaw().split(" ")[1]);
            for (int i = 0; i < numMessages; i++) {
                System.out.println(event.getTextChannel().getHistory().getRetrievedHistory().get(0).getContentRaw());
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

package command.commands.admin;

import command.Command;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;

public class Purge extends Command {

    /**
     * Initializes the command's key to "!purge".
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
    public void start(MessageReceivedEvent event) {
        if (!event.getMember().getRoles().contains(event.getGuild().getRoleById("486635066928136194"))
                && !event.getMember().isOwner()) {
            return;
        }

        String[] strings = event.getMessage().getContentRaw().split(" ");

        try {
            if (strings.length == 2) {
                int numMessages = Integer.parseInt(strings[1]);
                MessageHistory history = new MessageHistory(event.getTextChannel());
                List<Message> messages = history.retrievePast(numMessages + 1).complete();
                event.getTextChannel().deleteMessages(messages).queue();
            } else if (strings.length == 3) {
                int numMessages = Integer.parseInt(strings[2]);
                MessageHistory history = new MessageHistory(event.getTextChannel());
                ArrayList<Message> toDelete = new ArrayList<>();

                history.retrievePast(numMessages + 1).queue(historyMessages -> {
                    for (Message message : historyMessages) {
                        if (message.getAuthor().getIdLong() == event.getMessage().getMentionedUsers().get(0).getIdLong()) {
                            toDelete.add(message);
                        }
                    }
                });
                event.getTextChannel().deleteMessages(toDelete).queue();
            }
        } catch (Exception e) {
            MessageHistory history = new MessageHistory(event.getTextChannel());
            List<Message> messages = history.retrievePast(1).complete();
            event.getTextChannel().deleteMessages(messages).queue();
        }
    }
}

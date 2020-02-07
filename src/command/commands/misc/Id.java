package command.commands.misc;

import command.Command;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Id extends Command {

    /**
     * Initializes the command's key to "!id".
     */
    public Id() {
        super("!id", true);
    }

    /**
     * Checks if a key string matches the command's key.
     * Match is true if string starts with the key "!id".
     *
     * @param string the user's input being compared to the key
     * @return true if the input starts with the key "!id"
     */
    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey());
    }

    /**
     * Searches the guild for a member with the specified user ID number
     * and prints their effective name.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        String[] content = event.getMessage().getContentRaw().split(" ");

        if (content.length != 2) {
            channel.sendMessage("Command: !id <user ID>\n"
                    + "`Example: !id 192882738527731712`").queue();
            return;
        }

        try {
            Member member = event.getGuild().getMemberById(Long.parseLong(content[1]));
            if (member == null) throw new NullPointerException();
            channel.sendMessage("Found user: " + member.getEffectiveName()).queue();
        } catch (Exception e) {
            channel.sendMessage("Could not find a user with that ID").queue();
        }
    }
}

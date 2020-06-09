package command.commands.admin;

import command.AdminCommand;
import command.Command;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Arrays;

public class Echo extends Command implements AdminCommand {

    /**
     * Initializes the command's key to "!echo".
     */
    public Echo() {
        super("!echo", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.contains(getKey());
    }

    @Override
    public void start(MessageReceivedEvent event) {
        if (!memberIsAdmin(event.getMember()))
            return;

        Message message = event.getMessage();
        String[] split = message.getContentRaw().split(" ");
        String string = Arrays.toString(Arrays.copyOfRange(split, 1, split.length));
        event.getChannel().sendMessage(string).queue();
        message.delete().queue();
    }
}

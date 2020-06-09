package command.commands.admin;

import command.AdminCommand;
import command.Command;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Echo extends Command implements AdminCommand {

    /**
     * Initializes the command's key to "!echo".
     */
    public Echo() {
        super("!echo", true);
    }

    @Override
    public void start(MessageReceivedEvent event) {
        if (!memberIsAdmin(event.getMember()))
            return;

        event.getChannel().sendMessage(event.getMessage().getContentRaw());
    }
}

package command.commands.admin;

import command.AdminCommand;
import command.Command;
import main.timertasks.DiscussionPurge;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Cleanup extends Command implements AdminCommand {
    /**
     * Initializes the command's key to "!cleanup"
     */
    public Cleanup() {
        super("!cleanup", true);
    }

    @Override
    public void start(MessageReceivedEvent event) {
        if (!memberIsAdmin(event.getMember()))
            return;

        new DiscussionPurge().run();
    }
}

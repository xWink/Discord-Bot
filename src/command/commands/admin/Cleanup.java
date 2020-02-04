package command.commands.admin;

import command.AdminCommand;
import main.timertasks.DiscussionPurge;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Cleanup extends AdminCommand {
    /**
     * Initializes the command's key to "!cleanup"
     */
    public Cleanup() {
        super("!cleanup", true);
    }

    @Override
    protected void runCommand(MessageReceivedEvent event) {
         new DiscussionPurge().run();
    }
}

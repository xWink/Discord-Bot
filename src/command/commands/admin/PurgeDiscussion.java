package command.commands.admin;

import command.AdminCommand;
import main.timertasks.DiscussionPurge;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class PurgeDiscussion extends AdminCommand {
    /**
     * Initializes the command's key to "!cleanup"
     */
    protected PurgeDiscussion() {
        super("!cleanup", true);
    }

    @Override
    protected void runCommand(MessageReceivedEvent event) {
         new DiscussionPurge().run();
    }
}

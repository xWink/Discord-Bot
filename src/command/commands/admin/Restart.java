package command.commands.admin;

import command.AdminCommand;
import main.RoleBot;
import main.Server;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Restart extends AdminCommand {

    /**
     * Initializes the command's key.
     */
    public Restart() {
        super("!restart", true);
    }

    /**
     * Restarts the bot.
     *
     * @param event the event that triggered the command
     */
    @Override
    protected void runCommand(MessageReceivedEvent event) {
        Server.getApi().shutdown();
        RoleBot.main(new String[0]);
    }
}

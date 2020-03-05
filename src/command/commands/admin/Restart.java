package command.commands.admin;

import command.AdminCommand;
import main.RoleBot;
import main.Server;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;
import java.util.ArrayList;

public class Restart extends AdminCommand {

    /**
     * Initializes the command's key to "!restart".
     */
    public Restart() {
        super("!restart", true);
    }

    @Override
    protected void runCommand(MessageReceivedEvent event) {
        try {
            Server.getApi().shutdownNow();

            /* Build command: java -jar application.jar */
            final ArrayList<String> command = new ArrayList<>();
            command.add("java -jar /home/botadmin/DiscordBot/build/libs/DiscordBot-2-all.jar");

            final ProcessBuilder builder = new ProcessBuilder(command);
            builder.start();
            System.exit(0);
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

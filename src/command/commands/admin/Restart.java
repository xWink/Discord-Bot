package command.commands.admin;

import command.AdminCommand;
import command.Command;
import main.Server;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;

public class Restart extends Command implements AdminCommand {

    /**
     * Initializes the command's key to "!restart".
     */
    public Restart() {
        super("!restart", true);
    }

    @Override
    public void start(MessageReceivedEvent event) {
        if (!AdminCommand.memberIsAdmin(event.getMember()))
            return;

        try {
            Server.API.shutdownNow();

            /* Build command: java -jar application.jar */
            final ArrayList<String> command = new ArrayList<>();
            command.add("java -jar DiscordBot-2-all.jar");

            final ProcessBuilder builder = new ProcessBuilder(command);
            builder.start();
            System.exit(0);
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

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
            final String javaBin = System.getProperty("java.home") + File.separator + "build" + File.separator + "libs";
            final File currentJar = new File(RoleBot.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            /* is it a jar file? */
            if (!currentJar.getName().endsWith(".jar"))
                return;

            /* Build command: java -jar application.jar */
            final ArrayList<String> command = new ArrayList<>();
 System.out.println(currentJar.getAbsolutePath());
            command.add(javaBin);
            command.add("-jar");
            command.add(currentJar.getPath());

            final ProcessBuilder builder = new ProcessBuilder(command);
            builder.start();
            System.exit(0);
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

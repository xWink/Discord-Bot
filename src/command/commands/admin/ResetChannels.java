package command.commands.admin;

import command.AdminCommand;
import main.Server;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Objects;

public class ResetChannels extends AdminCommand {

    /**
     * Initializes the command's key to "!resetchannels"
     */
    public ResetChannels() {
        super("!resetchannels", true);
    }

    @Override
    protected void runCommand(MessageReceivedEvent event) {
        TextChannel channels = Objects.requireNonNull(event.getGuild().getTextChannelById(Server.CHANNELS_CHANNEL_ID));
        try {
            File file = new File("../../res/channels.txt");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                channels.sendMessage(line).queue();
            }
            reader.close();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

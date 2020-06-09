package command.commands.admin;

import command.AdminCommand;
import command.Command;
import main.Server;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.Objects;

public class ResetChannels extends Command implements AdminCommand {

    /**
     * Initializes the command's key to "!resetchannels"
     */
    public ResetChannels() {
        super("!resetchannels", true);
    }

    /**
     * Prints the text from channels.txt into the Channels channel such that each line from channels.txt
     * is a separate message. This is used to reset the content of the Channels channel in the event of a
     * change to the list of courses.
     *
     * @param event the MessageReceived event sent by the admin that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        if (!memberIsAdmin(event.getMember()))
            return;

        TextChannel channels = Objects.requireNonNull(event.getGuild().getTextChannelById(Server.CHANNELS_CHANNEL_ID));
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource("channels.txt"));
        try {
            File file = new File(url.getFile());
            //File file = new File("../../res/channels.txt");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                channels.sendMessage(line.replace("\\n", "\n")).queue();
            }
            reader.close();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

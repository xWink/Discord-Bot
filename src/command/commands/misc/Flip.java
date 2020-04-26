package command.commands.misc;

import command.Command;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.imageio.ImageIO;
import java.io.File;
import java.net.URL;
import java.util.Objects;
import java.util.Random;

public class Flip extends Command {

    /**
     * Initializes the command's key to "!flip".
     */
    public Flip() {
        super("!flip", true);
    }

    /**
     * Flips a coin and prints a message saying what side the coin landed on.
     * The message includes an image of the coin.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        boolean isHeads = new Random().nextBoolean();
        ClassLoader loader = getClass().getClassLoader();
        String output = "";
        URL url;

        if (isHeads) {
            output += "Heads!";
            url = Objects.requireNonNull(loader.getResource("loonie_heads.png"));
        } else {
            output += "Tails!";
            url = Objects.requireNonNull(loader.getResource("loonie_tails.png"));
        }

        try {
            event.getChannel().sendMessage(output).addFile(new File(url.getFile())).queue();
        } catch (Exception e) {
            event.getChannel().sendMessage(output).queue();
        }
    }
}

package command.commands.misc;

import command.Command;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
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
        MessageAction message = event.getChannel().sendMessage(isHeads ? "Heads!" : "Tails!");

        try {
            BufferedImage image = ImageIO.read(Objects.requireNonNull(loader.getResourceAsStream(
                    isHeads ? "loonie_heads.png" : "loonie_tails.png")));

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(image, "png", os);

            message.addFile(os.toByteArray(), "coin.png");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            message.queue();
        }
    }
}

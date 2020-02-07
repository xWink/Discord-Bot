package command.commands.misc;

import command.Command;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;
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
        String output = "";
        String imgPath = new File("").getAbsolutePath();

        if (isHeads) {
            output += "Heads!";
            imgPath = imgPath.replace("build/libs", "res/loonie_heads.png");
        } else {
            output += "Tails!";
            imgPath = imgPath.replace("build/libs", "res/loonie_tails.png");
        }

        event.getChannel().sendMessage(output).addFile(new File(imgPath)).queue();
    }
}

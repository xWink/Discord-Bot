package command.commands.blackjack;

import command.Command;
import command.util.cards.HandOfCards;
import command.util.cards.PhotoCombine;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Objects;

public class MyHand extends Command {

    /**
     * Initializes the command's key.
     */
    public MyHand() {
        super("!myhand", false);
    }

    /**
     * Shows the player's hand of cards.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        BlackJackGame game = BlackJackList.getUserGame(event.getAuthor().getIdLong());
        if (game == null) {
            channel.sendMessage("You haven't started a game yet!\n"
                    + "To start a new one, say `!bet <amount>`").queue();
            return;
        }

        String playerHand = game.getPlayer().getHand().toString();
        MessageAction message = channel.sendMessage(event.getAuthor().getName() + "'s hand is: " + playerHand);

        byte[] image;
        if ((image = PhotoCombine.genPhoto(game.getPlayer().getHand().getAsList())) != null)
            message.addFile(image, "out.png").queue();
        message.queue();
    }
}

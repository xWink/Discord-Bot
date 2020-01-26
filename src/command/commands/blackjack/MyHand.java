package command.commands.blackjack;

import command.Command;
import command.util.cards.PhotoCombine;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.io.File;

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
        BlackJackGame game = BlackJackList.getUserGame(event.getAuthor().getIdLong());

        if (game == null) {
            event.getChannel().sendMessage("You haven't started a game yet!\n"
                    + "To start a new one, say `!bet <amount>`").queue();
            return;
        }

        if (PhotoCombine.genPhoto(game.getPlayer().getHand().getHand())) {
            event.getChannel().sendMessage(event.getAuthor().getName() + "'s hand is:")
                    .addFile(new File(System.getProperty("user.dir") + "\\res\\cards\\out.png")).queue();
        } else {
            event.getChannel().sendMessage(event.getAuthor().getName() + "'s hand is:"
                    + game.getPlayer().getHand().toString()).queue();
        }
    }
}

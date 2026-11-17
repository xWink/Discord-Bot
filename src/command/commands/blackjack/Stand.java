package command.commands.blackjack;

import command.Command;
import command.util.cards.CardMessage;
import command.util.cards.HandOfCards;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import command.util.game.BlackJackStore;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Stand extends Command {

    /**
     * Initializes the command's key to "!stand".
     */
    public Stand() {
        super("!stand", false);
    }

    /**
     * Command where player stands in blackjack, ending the game and distributing
     * rewards or losses based on game results.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        BlackJackGame game = BlackJackList.getUserGame(event.getAuthor().getIdLong());
        String author = event.getAuthor().getName();

        if (game == null) {
            event.getChannel().sendMessage("You haven't started a game yet!\n"
                    + "To start a new one, say `!bet <amount>`").queue();
            return;
        }

        try {
            synchronized (game) {
                if (game.isSettled()) return;
                int reward = BlackJackStore.settle(game);
                HandOfCards dealerHand = game.getDealer().getHand();

                // Show dealer hand
                CardMessage.createCardMessage(
                        event.getChannel(),
                        "Dealers hand: " + dealerHand.toString(),
                        dealerHand.getAsList()
                ).queue();

                // Show winnings/losses/push
                String output = "";
                if (reward > 0)
                    output += author + " wins! Earnings: " + reward + " *gc*";
                else if (reward < 0)
                    output += author + " lost. Losses: " + (-reward) + " *gc*";
                else
                    output += "Push, " + author + "'s stake is returned.";
                event.getChannel().sendMessage(output).queue();
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

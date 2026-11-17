package command.commands.blackjack;

import command.Command;
import command.util.cards.CardMessage;
import command.util.cards.HandOfCards;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import command.util.game.BlackJackStore;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Hit extends Command {

    /**
     * Initializes the command's key to "!hit".
     */
    public Hit() {
        super("!hit", false);
    }

    /**
     * Every command must be able to be activated based on the event.
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

        String name = event.getAuthor().getName();

        try {
            synchronized (game) {
                if (game.isSettled()) return;
                int playerValue = game.hit();
                game.touch();
                String output = "";
                HandOfCards playerHand = game.getPlayer().getHand();

                // Show player hand
                CardMessage.createCardMessage(
                        channel,
                        name + "'s hand is now: " + playerHand.toString(),
                        playerHand.getAsList()
                ).queue();

                // Game continues as normal if no blackjack or bust
                if (playerValue < 21) {
                    BlackJackStore.save(game);
                    return;
                }

                // Player either hit 21 (auto-stand) or busted (round ends, dealer does not play)
                output += playerValue == 21 ? "You got 21!\n" : "You busted.\n";

                int reward = BlackJackStore.settle(game);
                HandOfCards dealerHand = game.getDealer().getHand();
                if (playerValue <= 21 && dealerHand.getValue() > 21) {
                    output += "Dealer busted!\n";
                }

                if (reward > 0)
                    output += "You earned " + reward + " *gc*" + economy.Cosmetics.extras(game.getPlayer().getUserId()).effect;
                else if (reward < 0)
                    output += "You lost " + (-reward) + " *gc*";
                else
                    output += "Push, your stake is returned";
                output += "\nDealers hand: " + dealerHand.toString();

                CardMessage.createCardMessage(
                        channel,
                        output,
                        dealerHand.getAsList()
                ).queue();
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

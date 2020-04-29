package command.commands.blackjack;

import command.Command;
import command.util.cards.CardMessage;
import command.util.cards.HandOfCards;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Hit extends Command {

    private EconomyConnector ec;

    /**
     * Initializes the command's key to "!hit".
     */
    public Hit() {
        super("!hit", false);
        ec = new EconomyConnector();
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

        int playerValue = game.hit();
        String output = "";
        String name = event.getAuthor().getName();
        HandOfCards playerHand = game.getPlayer().getHand();

        try {
            // Show player hand
            CardMessage.createCardMessage(
                    channel,
                    name + "'s hand is now: " + playerHand.toString(),
                    playerHand.getAsList()
            ).queue();

            // Game continues as normal if no blackjack or bust
            if (playerValue < 21)
                return;

            // Player either won or busted
            output += playerValue == 21 ? "You got 21!\n" : "You busted.\n";

            // Check how much player won/lost
            int reward = game.checkWinner();

            // Check if dealer busted
            HandOfCards dealerHand = game.getDealer().getHand();
            if (dealerHand.getValue() > 21) {
                output += "Dealer busted!\n";
            }

            // Output player's winnings/losses
            if (reward > 0)
                output += "You earned " + reward + " *gc*";
            else if (reward < 0)
                output += "You lost " + (-reward) + " *gc*";
            else
                output += "Tie game, you didn't win or lose any money";
            output += "\nDealers hand: " + dealerHand.toString();

            // Show game results
            CardMessage.createCardMessage(
                    channel,
                    output,
                    dealerHand.getAsList()
            ).queue();

            // Update database
            if (reward != 0)
                ec.addOrRemoveMoney(event.getAuthor().getIdLong(), reward);

            BlackJackList.removeGame(game);
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

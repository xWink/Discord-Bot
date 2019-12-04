package command.commands;

import command.Command;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import command.util.economy.Listing;
import command.util.economy.Marketplace;

public class Market extends Command {

    /**
     * Initializes the command's key to "!market".
     */
    public Market() {
        super("!market", false);
    }

    /**
     * Displays the available listings on the marketplace.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        try {
            String output = "**Current Listings:**\n";
            int num = 1;
            for (Listing listing : new Marketplace(event.getGuild()).getListings()) {
                output = output.concat(num++ + ". " + listing.toString() + "\n");
            }
            event.getChannel().sendMessage(output).queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

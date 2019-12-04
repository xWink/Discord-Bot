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
        super("!market");
    }

    /**
     * Displays the available listings on the marketplace.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        String output = "**Current Listings:**\n";

        for (Listing listing : new Marketplace().getListings()) {
            output = output.concat(listing.toString() + "\n");
        }

        event.getChannel().sendMessage(output).queue();
    }
}

package command.commands;

import command.Command;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import command.util.economy.Listing;
import command.util.economy.Marketplace;
import command.util.economy.RoleListing;

public class Buy extends Command {

    private EconomyConnector ec;
    private Marketplace mp;

    /**
     * Initializes the command's key to "!buy".
     */
    public Buy() {
        super("!buy");
        ec = new EconomyConnector();
    }

    /**
     * Compares a string to the command's key and checks if that
     * string starts with the key.
     *
     * @param string the user's input being compared to the key
     * @return returns true if the key matches and false otherwise
     */
    @Override
    public boolean keyMatches(String string) {
        return string.startsWith(getKey());
    }

    /**
     * Allows a user to buy listings from the marketplace based on the
     * argument given after the "!buy" key. The argument given is the
     * listing # in the market that the user wishes to purchase.
     *
     * Invalid arguments result in an error message.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        mp = new Marketplace(event.getGuild());
        String[] strings = event.getMessage().getContentRaw().split(" ");
        MessageChannel channel = event.getChannel();
        long userId = event.getAuthor().getIdLong();

        if (!verifyInput(strings, channel)) return;

        try {
            Listing listing = mp.getListing(Integer.parseInt(strings[1]) - 1);

            if (!ec.canAfford(userId, listing.getCost())) {
                channel.sendMessage("You cannot afford this item.").queue();
                return;
            }

            if (listing.getClass() == RoleListing.class) {
                if (ec.userHasColour(userId)) {
                    channel.sendMessage("You already have a colour. "
                            + "If you would like to replace it **without** refund, "
                            + "please contact a moderator.").queue();
                    return;
                }

                // Assign role
                event.getGuild().getController().addSingleRoleToMember(event.getMember(),
                        ((RoleListing) listing).getRole()).queue();
                // Remove money
                ec.addOrRemoveMoney(userId, 0 - listing.getCost());
                // Set role and expiry in database
                ec.setRole(userId, (RoleListing) listing);
                channel.sendMessage("Enjoy your new colour! :)").queue();
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    /**
     * Verifies the user's input to ensure that it is of the format
     * "!buy {listing #}", where listing # is an integer within range
     * of the market listings.
     *
     * @param strings the list of strings that results from split(" ") on the user's input
     * @param channel the MessageChannel where the bot will send messages on error
     * @return true if the input is valid, false if the input is not
     */
    private boolean verifyInput(String[] strings, MessageChannel channel) {
        if (strings.length != 2) {
            channel.sendMessage("Usage: `!buy <listing #>`\nExample: `!buy 1`").queue();
            return false;
        }

        try {
            int index = Integer.parseInt(strings[1]);
            if (index > mp.getListings().size() || index < 1) {
                channel.sendMessage("Error: listing # must be within market range").queue();
                return false;
            }
        } catch (NumberFormatException e) {
            channel.sendMessage("Error: listing # must be an integer").queue();
            return false;
        }

        return true;
    }
}

package command.commands.economy;

import command.Command;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import economy.InsufficientFundsException;
import economy.ShopService;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import command.util.economy.Listing;
import command.util.economy.Marketplace;
import command.util.economy.RoleListing;

import java.sql.SQLException;

public class Buy extends Command {

    private EconomyConnector ec;
    private Marketplace mp;

    /**
     * Initializes the command's key to "!buy".
     */
    public Buy() {
        super("!buy", false);
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
        return string.toLowerCase().startsWith(getKey());
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
        if (event.getMember() == null)
            return;

        mp = new Marketplace(event.getGuild());
        String[] strings = event.getMessage().getContentRaw().split(" ");
        MessageChannel channel = event.getChannel();
        long userId = event.getAuthor().getIdLong();

        if (!verifyInput(strings, channel)) return;

        try {
            Listing listing = mp.getListing(Integer.parseInt(strings[1]) - 1);

            if (listing.getClass() != RoleListing.class) return;
            RoleListing roleListing = (RoleListing) listing;
            Role role = roleListing.getRole();

            if (ec.userHasColour(userId) && !isSameColour(userId, role)) {
                channel.sendMessage("You already have a colour. "
                        + "If you would like to replace it **without** refund, "
                        + "please contact a moderator.").queue();
                return;
            }

            // Charge and record a pending purchase first; nothing is announced until Discord confirms.
            String purchaseId = "buy:" + event.getMessageId();
            ShopService shop = ShopService.getInstance();
            try {
                shop.begin(purchaseId, userId, roleListing.toString(), listing.getCost(), role.getIdLong(),
                        (int) listing.getDuration());
            } catch (InsufficientFundsException e) {
                channel.sendMessage("You cannot afford this item.").queue();
                return;
            }

            event.getGuild().addRoleToMember(event.getMember(), role).queue(
                    ok -> {
                        try {
                            if (shop.complete(purchaseId, role.getName())) {
                                channel.sendMessage("Enjoy your new colour! :)").queue();
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                            channel.sendMessage("Your colour was granted but saving it failed. "
                                    + "Please contact a moderator with receipt `" + purchaseId + "`.").queue();
                        }
                    },
                    failure -> {
                        try {
                            shop.failAndRefund(purchaseId);
                            channel.sendMessage("Discord would not grant the colour, so you were refunded.").queue();
                        } catch (SQLException e) {
                            e.printStackTrace();
                            channel.sendMessage("The colour grant failed and the refund needs a moderator. "
                                    + "Receipt `" + purchaseId + "`.").queue();
                        }
                    });
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    /** Renewing the colour you already hold is allowed and extends it. */
    private boolean isSameColour(long userId, Role role) throws SQLException {
        return ec.getRoleId(userId) == role.getIdLong();
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

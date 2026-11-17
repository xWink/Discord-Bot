package command.commands.economy;

import command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
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
            StringBuilder rentals = new StringBuilder();
            StringBuilder permanent = new StringBuilder();
            int num = 1;
            for (Listing listing : new Marketplace(event.getGuild()).getListings()) {
                String line = "`" + num++ + ".` " + listing.toString() + "\n";
                if (listing.getDuration() >= 99999) permanent.append(line);
                else rentals.append(line);
            }
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.YELLOW);
            eb.setTitle("Market");
            eb.addField("Colour rentals (7 days)", rentals.toString(), false);
            eb.addField("Permanent colours", permanent.toString(), false);
            eb.setFooter("Buy with !buy <number>. Buying the colour you already rent extends it. "
                    + "Prices are in GryphCoins (gc).", null);
            event.getChannel().sendMessageEmbeds(eb.build()).queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

package command.commands.economy;

import command.Command;
import economy.CosmeticShop;
import economy.InsufficientFundsException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
import java.util.List;

/**
 * {@code !shop} lists cosmetics, {@code !shop buy <code>} purchases one, and
 * {@code !shop owned} lists what you have.
 */
public class ShopCommand extends Command {

    public ShopCommand() {
        super("!shop", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey());
    }

    @Override
    public void start(MessageReceivedEvent event) {
        String[] args = event.getMessage().getContentRaw().split("\\s+");
        try {
            CosmeticShop shop = CosmeticShop.getInstance();
            long user = event.getAuthor().getIdLong();
            if (args.length >= 3 && args[1].equalsIgnoreCase("buy")) {
                try {
                    CosmeticShop.Item item = shop.buy(user, args[2].toLowerCase(), "item:" + event.getMessageId());
                    event.getChannel().sendMessage(item == null
                            ? "That item does not exist, or you already own it."
                            : "Bought **" + item.name + "** for " + item.price + " GC.").queue();
                } catch (InsufficientFundsException e) {
                    event.getChannel().sendMessage("You cannot afford that.").queue();
                }
            } else if (args.length == 2 && args[1].equalsIgnoreCase("owned")) {
                List<String> owned = shop.owned(user);
                event.getChannel().sendMessage(owned.isEmpty() ? "You own no cosmetics yet."
                        : "Your cosmetics:\n" + String.join("\n", owned)).queue();
            } else {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Color.YELLOW);
                eb.setTitle("Cosmetic shop");
                StringBuilder featured = new StringBuilder();
                StringBuilder regular = new StringBuilder();
                for (CosmeticShop.Item item : shop.catalog()) {
                    String line = "`" + item.code + "` " + item.name + " — **" + item.price + " GC**"
                            + (item.durationMs == 0 ? " (permanent)" : " (" + item.durationMs / 3600000 + "h)") + "\n";
                    (item.featured ? featured : regular).append(line);
                }
                eb.addField("Featured", featured.length() == 0 ? "None this week." : featured.toString(), false);
                eb.addField("Always available", regular.length() == 0 ? "None." : regular.toString(), false);
                eb.setFooter("!shop buy <code> · !shop owned · Cosmetics never change payouts or rankings.", null);
                event.getChannel().sendMessage(eb.build()).queue();
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

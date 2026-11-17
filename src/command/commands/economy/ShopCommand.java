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
                buy(event, shop, user, args[2].toLowerCase());
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
                            + (item.featured && item.previousPrice > 0 && item.previousPrice != item.price
                            ? " (was " + item.previousPrice + ")" : "")
                            + (item.durationMs == 0 ? " (permanent)" : " (" + item.durationMs / 3600000 + "h)") + "\n";
                    (item.featured ? featured : regular).append(line);
                }
                eb.addField("Featured · prices refresh <t:" + nextRefresh() + ":R>",
                        featured.length() == 0 ? "None this week." : featured.toString(), false);
                eb.addField("Always available", regular.length() == 0 ? "None." : regular.toString(), false);
                eb.setFooter("!shop buy <code> (featured items show a price to confirm first) · !shop owned · "
                        + "Cosmetics never change payouts or rankings.", null);
                event.getChannel().sendMessage(eb.build()).queue();
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    private static final long QUOTE_MS = 2 * 60 * 1000;
    private static final java.util.Map<String, long[]> QUOTES = new java.util.concurrent.ConcurrentHashMap<>();

    /** Next Monday 00:00 Eastern, in epoch seconds. */
    private static long nextRefresh() {
        java.time.ZoneId zone = java.time.ZoneId.of("America/New_York");
        return java.time.LocalDate.now(zone).with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))
                .atStartOfDay(zone).toEpochSecond();
    }

    /**
     * Featured items are bought in two steps: the first call quotes the price and holds it for two
     * minutes, the second confirms. If the price moved in between, the buyer is shown the new price.
     */
    private void buy(MessageReceivedEvent event, CosmeticShop shop, long user, String code) throws Exception {
        CosmeticShop.Item item = null;
        for (CosmeticShop.Item i : shop.catalog()) if (i.code.equals(code)) item = i;
        if (item == null) {
            event.getChannel().sendMessage("That item does not exist.").queue();
            return;
        }
        String key = user + ":" + code;
        long[] quote = QUOTES.get(key);
        boolean quoted = quote != null && System.currentTimeMillis() - quote[1] <= QUOTE_MS;
        if (item.featured && !quoted) {
            QUOTES.put(key, new long[]{item.price, System.currentTimeMillis()});
            event.getChannel().sendMessage("**" + item.name + "** costs **" + item.price + " GC**"
                    + (item.previousPrice > 0 && item.previousPrice != item.price ? " (was " + item.previousPrice + ")" : "")
                    + ". Say `!shop buy " + code + "` again within 2 minutes to confirm.").queue();
            return;
        }
        try {
            CosmeticShop.Item bought = shop.buy(user, code, "item:" + event.getMessageId(),
                    item.featured ? Integer.valueOf((int) quote[0]) : null);
            QUOTES.remove(key);
            event.getChannel().sendMessage(bought == null ? "You already own that." : "Bought **" + bought.name
                    + "** for " + (item.featured ? (int) quote[0] : bought.price) + " GC. Receipt `item:"
                    + event.getMessageId() + "`.").queue();
        } catch (economy.PriceChangedException e) {
            QUOTES.put(key, new long[]{e.getNewPrice(), System.currentTimeMillis()});
            event.getChannel().sendMessage("The price changed to **" + e.getNewPrice() + " GC**. Say `!shop buy "
                    + code + "` again to confirm.").queue();
        } catch (InsufficientFundsException e) {
            event.getChannel().sendMessage("You cannot afford that.").queue();
        }
    }
}

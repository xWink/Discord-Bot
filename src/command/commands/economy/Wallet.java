package command.commands.economy;

import command.Command;
import economy.DecayMath;
import economy.DecayService;
import economy.EconomyService;
import economy.LedgerEntry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class Wallet extends Command {

    private static final int RECEIPTS_SHOWN = 8;

    /**
     * Initializes the command's key to "!wallet".
     */
    public Wallet() {
        super("!wallet", true);
    }

    /**
     * Sends your wallet by direct message (or in the channel if DMs are closed). Shows spendable GryphCoins first, then reserved coins, the estimated
     * next decay, and the latest receipts.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        long userId = event.getAuthor().getIdLong();
        try {
            EconomyService service = EconomyService.getInstance();
            int balance = service.getBalance(userId);
            int reserved = service.getReserved(userId);
            double decay = DecayService.estimateNext(userId);
            Long firstWeek = DecayService.getFirstWeek();
            List<LedgerEntry> history = service.getHistory(userId, RECEIPTS_SHOWN);

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.YELLOW);
            eb.setTitle(event.getAuthor().getName() + "'s wallet");
            eb.setThumbnail(event.getAuthor().getAvatarUrl());
            eb.addField("Spendable", (balance - reserved) + " GC", true);
            eb.addField("Reserved", reserved + " GC", true);
            eb.addField("Total", balance + " GC", true);
            eb.addField("Decay", decayText(decay, firstWeek), false);
            eb.addField("Recent receipts", receiptText(history), false);
            eb.setFooter("Receipt IDs are shown so a moderator can look up any change.", null);
            // Balances are private by default: DM the embed, and fall back to the channel if DMs are closed.
            net.dv8tion.jda.api.entities.MessageEmbed embed = eb.build();
            event.getAuthor().openPrivateChannel().queue(
                    pm -> pm.sendMessage(embed).queue(
                            ok -> {
                                if (event.isFromGuild()) event.getMessage().addReaction("\uD83D\uDCEC").queue();
                            },
                            fail -> event.getChannel().sendMessage(embed).queue()),
                    fail -> event.getChannel().sendMessage(embed).queue());
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    private static String decayText(double estimate, Long firstWeek) {
        if (firstWeek == null) {
            return "Not active. Balances above " + DecayMath.PROTECTED + " GC lose 2% a week once it starts.";
        }
        String when = formatDate(DecayService.weekStart(firstWeek.intValue()));
        boolean started = System.currentTimeMillis() >= DecayService.weekStart(firstWeek.intValue());
        String estimateText = String.format("%.2f GC", estimate);
        return (started ? "Next weekly charge: about " : "Starts " + when + ". It would charge about ")
                + estimateText + " (2% of the part above " + DecayMath.PROTECTED + " GC).";
    }

    private static String receiptText(List<LedgerEntry> history) {
        if (history.isEmpty()) return "No activity yet.";
        StringBuilder sb = new StringBuilder();
        for (LedgerEntry e : history) {
            sb.append(e.getDelta() >= 0 ? "+" : "").append(e.getDelta()).append(" GC  ")
                    .append(e.getReason()).append("  (balance ").append(e.getBalanceAfter())
                    .append(")  `").append(shorten(e.getOpId())).append("`\n");
        }
        return sb.toString();
    }

    private static String shorten(String opId) {
        return opId.length() > 24 ? opId.substring(0, 24) + "…" : opId;
    }

    private static String formatDate(long ms) {
        SimpleDateFormat df = new SimpleDateFormat("EEE MMM d");
        df.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        return df.format(new Date(ms));
    }
}

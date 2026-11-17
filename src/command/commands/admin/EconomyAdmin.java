package command.commands.admin;

import command.AdminCommand;
import command.Command;
import economy.DecayService;
import economy.EconomyService;
import economy.InsufficientFundsException;
import economy.LedgerEntry;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Moderator tools: look up a receipt, issue a documented correction, and
 * publish the first decay cutoff.
 */
public class EconomyAdmin extends Command implements AdminCommand {

    public EconomyAdmin() {
        super("!economy", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey());
    }

    @Override
    public void start(MessageReceivedEvent event) {
        if (!memberIsAdmin(event.getMember()))
            return;

        String[] args = event.getMessage().getContentRaw().split("\\s+");
        try {
            if (args.length >= 3 && args[1].equalsIgnoreCase("receipt")) {
                receipt(event, args[2]);
            } else if (args.length >= 5 && args[1].equalsIgnoreCase("adjust")
                    && !event.getMessage().getMentionedUsers().isEmpty()) {
                adjust(event, args);
            } else if (args.length == 3 && args[1].equalsIgnoreCase("decay")) {
                decay(event, Integer.parseInt(args[2]));
            } else {
                event.getChannel().sendMessage("Usage:\n`!economy receipt <id>`\n"
                        + "`!economy adjust <@user> <+/-amount> <note>`\n"
                        + "`!economy decay <weeks from now>` (at least 1, leaving a preview week)").queue();
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    private void receipt(MessageReceivedEvent event, String opId) throws Exception {
        List<LedgerEntry> entries = EconomyService.getInstance().findByOpId(opId);
        if (entries.isEmpty()) {
            event.getChannel().sendMessage("No receipt with that ID.").queue();
            return;
        }
        StringBuilder sb = new StringBuilder("Receipt `" + opId + "`:\n");
        for (LedgerEntry e : entries) {
            sb.append(e.getDelta() >= 0 ? "+" : "").append(e.getDelta()).append(" GC, ").append(e.getReason())
                    .append(", balance after ").append(e.getBalanceAfter())
                    .append(", ref ").append(e.getRef()).append('\n');
        }
        event.getChannel().sendMessage(sb.toString()).queue();
    }

    private void adjust(MessageReceivedEvent event, String[] args) throws Exception {
        long target = event.getMessage().getMentionedUsers().get(0).getIdLong();
        int amount = Integer.parseInt(args[3].replace("+", ""));
        String note = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
        String opId = "admin:" + event.getMessageId();
        String ref = event.getAuthor().getId() + ":" + note;
        if (amount == 0) return;
        try {
            if (amount > 0) EconomyService.getInstance().credit(target, amount, "admin:correction", ref, opId);
            else EconomyService.getInstance().debit(target, -amount, "admin:correction", ref, opId);
        } catch (InsufficientFundsException e) {
            event.getChannel().sendMessage("That wallet only has " + e.getSpendable() + " GC spendable.").queue();
            return;
        }
        event.getChannel().sendMessage("Correction recorded. Receipt `" + opId + "`.").queue();
    }

    private void decay(MessageReceivedEvent event, int weeksFromNow) throws Exception {
        if (weeksFromNow < 1) {
            event.getChannel().sendMessage("Leave at least one preview week.").queue();
            return;
        }
        int week = DecayService.weekIndex(System.currentTimeMillis()) + weeksFromNow;
        DecayService.setFirstWeek(week);
        event.getChannel().sendMessage("First decay cutoff set for <t:"
                + DecayService.weekStart(week) / 1000 + ":F>. `!wallet` now shows estimates.").queue();
    }
}

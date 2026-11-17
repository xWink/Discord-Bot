package command.commands.economy;

import command.Command;
import economy.BountyService;
import economy.InsufficientFundsException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

/**
 * {@code !bounty post <reward> <job>}, {@code list}, {@code claim <id>},
 * {@code accept <id>}, {@code cancel <id>}.
 */
public class BountyCommand extends Command {

    public BountyCommand() {
        super("!bounty", true);
    }

    @Override
    public boolean keyMatches(String s) {
        return s.toLowerCase().startsWith("!bounty");
    }

    @Override
    public void start(MessageReceivedEvent event) {
        String[] a = event.getMessage().getContentRaw().split("\\s+");
        BountyService svc = BountyService.getInstance();
        long user = event.getAuthor().getIdLong();
        try {
            String sub = a.length >= 2 ? a[1].toLowerCase() : "list";
            if (sub.equals("post") && a.length >= 4) {
                int reward = Integer.parseInt(a[2]);
                if (reward < 1 || reward > BountyService.MAX_REWARD) {
                    event.getChannel().sendMessage("Rewards run from 1 to " + BountyService.MAX_REWARD + " GC.")
                            .queue();
                    return;
                }
                String desc = String.join(" ", Arrays.copyOfRange(a, 3, a.length));
                if (desc.length() > 200) desc = desc.substring(0, 200);
                try {
                    long id = svc.post(user, reward, desc);
                    event.getChannel().sendMessage(id < 0 ? "You already have " + BountyService.MAX_OPEN_PER_POSTER
                            + " open bounties." : "Bounty #" + id + " posted. " + reward + " GC is reserved until "
                            + "you accept, cancel, or it expires in 7 days.").queue();
                } catch (InsufficientFundsException e) {
                    event.getChannel().sendMessage("You do not have " + reward + " GC spendable.").queue();
                }
            } else if (sub.equals("claim") && a.length == 3) {
                event.getChannel().sendMessage(svc.claim(Long.parseLong(a[2]), user)
                        ? "Claimed. Do the work, then ask the poster to `!bounty accept " + a[2] + "`."
                        : "That bounty is not open to you.").queue();
            } else if (sub.equals("accept") && a.length == 3) {
                long worker = svc.accept(Long.parseLong(a[2]), user);
                event.getChannel().sendMessage(worker == 0 ? "You have no claimed bounty with that number."
                        : "Paid out to <@" + worker + ">.").queue();
            } else if (sub.equals("cancel") && a.length == 3) {
                event.getChannel().sendMessage(svc.cancel(Long.parseLong(a[2]), user)
                        ? "Cancelled and refunded." : "Only your own unclaimed bounties can be cancelled.").queue();
            } else {
                List<BountyService.Bounty> open = svc.open();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Color.YELLOW);
                eb.setTitle("Bounty board");
                StringBuilder sb = new StringBuilder();
                for (BountyService.Bounty b : open) {
                    sb.append("`#").append(b.id).append("` **").append(b.reward).append(" GC** — ")
                            .append(b.description).append(" (by <@").append(b.poster).append(">").append(economy.Cosmetics.tag(b.poster))
                            .append(b.status.equals("CLAIMED") ? ", claimed" : "").append(")\n");
                }
                eb.setDescription(open.isEmpty() ? "No open bounties." : sb.toString());
                eb.setFooter("!bounty post <reward> <job> · claim <id> · accept <id> · cancel <id>", null);
                event.getChannel().sendMessageEmbeds(eb.build()).queue();
            }
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Check the numbers in your command.").queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

package command.commands.economy;

import command.Command;
import command.util.economy.Listing;
import command.util.economy.Marketplace;
import command.util.economy.RoleListing;
import economy.InstallmentService;
import economy.InsufficientFundsException;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * {@code !installment}, {@code !installment start <colour>},
 * {@code !installment pay <amount> confirm}, {@code !installment finish}.
 */
public class InstallmentCommand extends Command {

    public InstallmentCommand() {
        super("!installment", false);
    }

    @Override
    public boolean keyMatches(String s) {
        return s.toLowerCase().startsWith("!installment");
    }

    @Override
    public void start(MessageReceivedEvent event) {
        if (event.getMember() == null) return;
        String[] a = event.getMessage().getContentRaw().split("\\s+");
        long user = event.getAuthor().getIdLong();
        InstallmentService svc = InstallmentService.getInstance();
        try {
            InstallmentService.Plan plan = svc.current(user);
            String sub = a.length >= 2 ? a[1].toLowerCase() : "";
            if (sub.equals("start") && a.length == 3) {
                RoleListing listing = find(event, a[2]);
                if (listing == null) {
                    event.getChannel().sendMessage("Pick one of the permanent colours in `!market`.").queue();
                } else if (svc.start(user, listing.getRole().getIdLong(), listing.getRole().getName()) == null) {
                    event.getChannel().sendMessage("You already have a plan in progress.").queue();
                } else {
                    event.getChannel().sendMessage("Plan started for **" + listing.getRole().getName() + "** ("
                            + InstallmentService.PRICE + " GC). Pay with `!installment pay <amount> confirm`. "
                            + "Installments are final and the plan cannot be transferred.").queue();
                }
            } else if (sub.equals("pay") && a.length >= 3) {
                int amount = Integer.parseInt(a[2]);
                if (plan == null || !plan.status.equals("ACTIVE") || amount <= 0) {
                    event.getChannel().sendMessage("You have no active plan, or the amount is invalid.").queue();
                } else if (a.length < 4 || !a[3].equalsIgnoreCase("confirm")) {
                    event.getChannel().sendMessage("This will pay **" + Math.min(amount, plan.target - plan.paid)
                            + " GC** toward your " + plan.colour + " colour. It is final and cannot be refunded.\n"
                            + "Say `!installment pay " + amount + " confirm` to go ahead.").queue();
                } else {
                    try {
                        InstallmentService.Plan after = svc.pay(user, amount, "inst:" + event.getMessageId());
                        event.getChannel().sendMessage(progress(after)).queue();
                        if (after != null && after.status.equals("GRANTING")) grant(event, after);
                    } catch (InsufficientFundsException e) {
                        event.getChannel().sendMessage("You only have " + e.getSpendable() + " spendable GC.").queue();
                    }
                }
            } else if (sub.equals("finish")) {
                if (plan != null && plan.status.equals("GRANTING")) grant(event, plan);
                else event.getChannel().sendMessage("Nothing waiting for delivery.").queue();
            } else {
                event.getChannel().sendMessage(plan == null
                        ? "No plan. Start one with `!installment start <colour>`." : progress(plan)).queue();
            }
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("The amount must be a whole number.").queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    private static String progress(InstallmentService.Plan p) {
        if (p == null) return "No active plan.";
        int pct = p.paid * 100 / p.target;
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 20; i++) bar.append(i < pct / 5 ? '█' : '░');
        return "**" + p.colour + "** permanent colour\n`" + bar + "` " + p.paid + " / " + p.target + " GC ("
                + pct + "%)" + (p.status.equals("GRANTING") ? "\nFully paid. Delivering your colour..." : "");
    }

    private static RoleListing find(MessageReceivedEvent event, String name) {
        for (Listing l : new Marketplace(event.getGuild()).getListings()) {
            if (l instanceof RoleListing && l.getDuration() >= 99999
                    && ((RoleListing) l).getRole().getName().equalsIgnoreCase(name)) return (RoleListing) l;
        }
        return null;
    }

    /** Role grant; a failure leaves the plan fully paid so {@code !installment finish} can retry. */
    private static void grant(MessageReceivedEvent event, InstallmentService.Plan plan) {
        Role role = event.getGuild().getRoleById(plan.roleId);
        Member member = event.getMember();
        if (role == null) {
            event.getChannel().sendMessage("That colour role no longer exists. A moderator must help; your "
                    + plan.paid + " GC is safe on plan #" + plan.id + ".").queue();
            return;
        }
        event.getGuild().addRoleToMember(member, role).queue(ok -> {
            try {
                if (InstallmentService.getInstance().markGranted(plan.id)) {
                    event.getChannel().sendMessage("Your permanent **" + plan.colour + "** colour is yours!").queue();
                }
            } catch (Exception e) {
                e.printStackTrace();
                event.getChannel().sendMessage("The colour was granted but saving failed. Contact a moderator, "
                        + "plan #" + plan.id + ".").queue();
            }
        }, failure -> event.getChannel().sendMessage("Discord would not grant the colour yet. You are fully paid; "
                + "say `!installment finish` to retry.").queue());
    }
}

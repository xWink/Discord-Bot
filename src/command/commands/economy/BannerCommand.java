package command.commands.economy;

import command.AdminCommand;
import command.Command;
import economy.BannerService;
import economy.InsufficientFundsException;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * {@code !banner request @TeamRole}, then someone on that team (or staff) says
 * {@code !banner approve <id>}. The price stays reserved until approved and is
 * refunded if declined or unanswered for 48 hours.
 */
public class BannerCommand extends Command implements AdminCommand {

    public BannerCommand() {
        super("!banner", true);
    }

    @Override
    public boolean keyMatches(String s) {
        return s.toLowerCase().startsWith("!banner");
    }

    @Override
    public void start(MessageReceivedEvent event) {
        if (event.getMember() == null) return;
        String[] a = event.getMessage().getContentRaw().split("\\s+");
        BannerService svc = BannerService.getInstance();
        long user = event.getAuthor().getIdLong();
        try {
            String sub = a.length >= 2 ? a[1].toLowerCase() : "";
            if (sub.equals("request") && event.getMessage().getMentionedRoles().size() == 1) {
                Role team = event.getMessage().getMentionedRoles().get(0);
                try {
                    long id = svc.request(user, team.getIdLong(), team.getName());
                    event.getChannel().sendMessage(id < 0 ? "You already have a request waiting."
                            : "Request #" + id + " sent. " + BannerService.PRICE + " GC is reserved. A member of "
                            + team.getAsMention() + " (or staff) can say `!banner approve " + id + "` within 48 hours, "
                            + "otherwise you are refunded.").queue();
                } catch (InsufficientFundsException e) {
                    event.getChannel().sendMessage("A team banner costs " + BannerService.PRICE + " GC.").queue();
                }
            } else if ((sub.equals("approve") || sub.equals("decline")) && a.length == 3) {
                BannerService.Request r = svc.get(Long.parseLong(a[2]));
                if (r == null || !r.status.equals("PENDING")) {
                    event.getChannel().sendMessage("No pending request with that number.").queue();
                    return;
                }
                Role role = event.getGuild().getRoleById(r.roleId);
                Member m = event.getMember();
                boolean onTeam = role != null && m.getRoles().contains(role) && r.user != user;
                boolean owner = r.user == user && sub.equals("decline");
                if (!(onTeam || memberIsAdmin(m) || owner)) {
                    event.getChannel().sendMessage("Only the team, staff, or the requester (to withdraw) can do that.")
                            .queue();
                    return;
                }
                boolean ok = sub.equals("approve") ? svc.approve(r.id) : svc.decline(r.id);
                event.getChannel().sendMessage(!ok ? "That request was already handled."
                        : sub.equals("approve") ? "Approved. <@" + r.user + "> now has the " + r.team
                        + " banner for a week. Wear it with `!equip banner-club`."
                        : "Declined and refunded.").queue();
            } else {
                event.getChannel().sendMessage("`!banner request @TeamRole` · `!banner approve <id>` · "
                        + "`!banner decline <id>`. " + BannerService.PRICE + " GC for a week.").queue();
            }
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("The request number must be a number.").queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

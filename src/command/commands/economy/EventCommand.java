package command.commands.economy;

import command.AdminCommand;
import command.Command;
import economy.EventService;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@code !event start <name>} (staff, or a player holding an event hosting
 * pass), {@code !event confirm @a @b}, {@code !event end}.
 */
public class EventCommand extends Command implements AdminCommand {

    public EventCommand() {
        super("!event", true);
    }

    @Override
    public boolean keyMatches(String s) {
        return s.toLowerCase().startsWith("!event");
    }

    @Override
    public void start(MessageReceivedEvent event) {
        String[] a = event.getMessage().getContentRaw().split("\\s+");
        long host = event.getAuthor().getIdLong();
        EventService svc = EventService.getInstance();
        try {
            String sub = a.length >= 2 ? a[1].toLowerCase() : "";
            if (sub.equals("start") && a.length >= 3) {
                long id = svc.start(host, String.join(" ", Arrays.copyOfRange(a, 2, a.length)),
                        memberIsAdmin(event.getMember()));
                event.getChannel().sendMessage(id == -1 ? "You already have an event running."
                        : id == -2 ? "Hosting needs an event hosting pass (`!shop buy event-pass`)."
                        : "Event #" + id + " started. Confirm players with `!event confirm @a @b`, then "
                        + "`!event end` to pay " + EventService.PAYOUT + " GC each (max " + EventService.WEEKLY_CAP
                        + " GC a week).").queue();
            } else if (sub.equals("confirm")) {
                List<Long> users = new ArrayList<>();
                event.getMessage().getMentionedUsers().forEach(u -> {
                    if (!u.isBot()) users.add(u.getIdLong());
                });
                int added = svc.confirm(host, users);
                event.getChannel().sendMessage(added < 0 ? "You have no event running."
                        : added + " new participants confirmed.").queue();
            } else if (sub.equals("end")) {
                EventService.Result r = svc.end(host);
                event.getChannel().sendMessage(r == null ? "You have no event running."
                        : "Event over. " + r.participants + " participants, " + r.totalPaid + " GC paid out.")
                        .queue();
            } else {
                event.getChannel().sendMessage("`!event start <name>` · `!event confirm @players` · `!event end`")
                        .queue();
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

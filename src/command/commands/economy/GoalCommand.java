package command.commands.economy;

import command.Command;
import economy.GoalService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;

/**
 * {@code !goal} shows the community goal; contributions are made with
 * {@link Contribute}.
 */
public class GoalCommand extends Command {

    public GoalCommand() {
        super("!goal", true);
    }

    @Override
    public void start(MessageReceivedEvent event) {
        try {
            GoalService.Goal goal = GoalService.getInstance().current();
            if (goal == null) {
                event.getChannel().sendMessage("There is no community goal right now.").queue();
                return;
            }
            event.getChannel().sendMessageEmbeds(embed(goal).build()).queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    static EmbedBuilder embed(GoalService.Goal goal) {
        int pct = goal.target == 0 ? 0 : Math.min(100, goal.raised * 100 / goal.target);
        int filled = pct / 5;
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 20; i++) bar.append(i < filled ? '█' : '░');

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(goal.status.equals(GoalService.ACTIVE) ? Color.YELLOW : Color.GREEN);
        eb.setTitle("Community goal: " + goal.title);
        eb.setDescription("`" + bar + "` " + pct + "%\n**" + goal.raised + " / " + goal.target + " GC** raised");
        eb.addField("Contributors", Integer.toString(goal.contributors), true);
        eb.addField("Status", goal.status.charAt(0) + goal.status.substring(1).toLowerCase(), true);
        eb.addField("When it fills", "The unlock is scheduled and announced once. Everyone can take part.", false);
        eb.setFooter("Say !contribute <amount> to help. Contributions are final.", null);
        return eb;
    }
}

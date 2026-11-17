package command.commands.economy;

import arcade.ArcadeService;
import arcade.PunchCard;
import command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
import java.util.List;

/**
 * Weekly arcade board: top 10 plus the viewer's own rank and distance to the
 * next place. Ties break on faster average correct time.
 */
public class ArcadeScores extends Command {

    private static final int TOP = 10;

    public ArcadeScores() {
        super("!arcadescores", true);
    }

    @Override
    public void start(MessageReceivedEvent event) {
        try {
            List<ArcadeService.Standing> board = ArcadeService.weeklyBoard();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.YELLOW);
            eb.setTitle("Arcade leaderboard · this week");
            if (board.isEmpty()) {
                eb.setDescription("No rounds played yet this week. Say `!arcade` to get on the board.");
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(TOP, board.size()); i++) {
                    ArcadeService.Standing s = board.get(i);
                    sb.append(s.rank).append(". ").append(name(event, s.user)).append(" — ")
                            .append(s.correct).append("/").append(s.played).append(" correct, avg ")
                            .append(String.format("%.1f", s.avgMs / 1000.0)).append("s\n");
                }
                eb.setDescription(sb.toString());
                eb.addField("You", viewerLine(event.getAuthor().getIdLong(), board), false);
            }
            eb.setFooter("Ties are broken by faster average time on correct answers. Free and practice rounds "
                    + "beyond " + ArcadeService.RANKED_PER_DAY_CAP + " a day are unranked.", null);
            event.getChannel().sendMessage(eb.build()).queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    private static String viewerLine(long user, List<ArcadeService.Standing> board) {
        for (int i = 0; i < board.size(); i++) {
            ArcadeService.Standing s = board.get(i);
            if (s.user != user) continue;
            String line = "Rank " + s.rank + " with " + s.correct + " correct.";
            if (i > 0) {
                int gap = board.get(i - 1).correct - s.correct;
                line += gap > 0 ? " " + gap + " more to pass the next rank." : " Level on points; a faster average passes.";
            }
            return line;
        }
        return "Not ranked yet this week. Punch card: " + safeDays(user) + "/" + PunchCard.DAYS_NEEDED + " days.";
    }

    private static int safeDays(long user) {
        try {
            return PunchCard.daysThisWeek(user);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String name(MessageReceivedEvent event, long user) {
        net.dv8tion.jda.api.entities.User u = event.getJDA().getUserById(user);
        return u != null ? economy.Cosmetics.nameWithTitle(user, u.getName()) : "<@" + user + ">";
    }
}

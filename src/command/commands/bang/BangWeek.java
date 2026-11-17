package command.commands.bang;

import command.Command;
import database.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Weekly bang board built from timestamped results. Survival rankings need at
 * least {@value #MIN_ATTEMPTS} attempts in the week, and each row shows its
 * sample size. Lifetime counters in {@code !bangscore} are unchanged.
 */
public class BangWeek extends Command {

    static final int MIN_ATTEMPTS = 20;
    private static final int TOP = 10;
    private static final ZoneId ZONE = ZoneId.of("America/New_York");

    public BangWeek() {
        super("!bangweek", true);
    }

    private static final class Row {
        final long user;
        final int attempts;
        final int deaths;

        Row(long user, int attempts, int deaths) {
            this.user = user;
            this.attempts = attempts;
            this.deaths = deaths;
        }

        double survival() {
            return 100.0 * (attempts - deaths) / attempts;
        }
    }

    @Override
    public void start(MessageReceivedEvent event) {
        try {
            long since = LocalDate.now(ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay(ZONE).toInstant().toEpochMilli();
            List<Row> rows = Database.inTransaction(c -> {
                List<Row> list = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement("SELECT user, COUNT(*), SUM(died) FROM bang_results "
                        + "WHERE played_at >= ? GROUP BY user HAVING COUNT(*) >= ? "
                        + "ORDER BY 1 - SUM(died) / COUNT(*) DESC, COUNT(*) DESC, user")) {
                    ps.setLong(1, since);
                    ps.setInt(2, MIN_ATTEMPTS);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) list.add(new Row(rs.getLong(1), rs.getInt(2), rs.getInt(3)));
                    }
                }
                return list;
            });

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.YELLOW);
            eb.setTitle("Bang survival · this week");
            if (rows.isEmpty()) {
                eb.setDescription("Nobody has " + MIN_ATTEMPTS + " attempts this week yet.");
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(TOP, rows.size()); i++) {
                    Row r = rows.get(i);
                    net.dv8tion.jda.api.entities.User u = event.getJDA().getUserById(r.user);
                    sb.append(i + 1).append(". ").append(u != null ? u.getName() : "<@" + r.user + ">")
                            .append(" — ").append(String.format("%.1f", r.survival())).append("% (")
                            .append(r.attempts).append(" attempts)\n");
                }
                eb.setDescription(sb.toString());
                for (int i = 0; i < rows.size(); i++) {
                    if (rows.get(i).user == event.getAuthor().getIdLong()) {
                        eb.addField("You", "Rank " + (i + 1) + " of " + rows.size() + " with "
                                + String.format("%.1f", rows.get(i).survival()) + "%", false);
                    }
                }
            }
            eb.setFooter("Needs " + MIN_ATTEMPTS + "+ attempts this week. Ties go to more attempts. "
                    + "Counting started when weekly tracking was added.", null);
            event.getChannel().sendMessage(eb.build()).queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

package courses;

import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.Color;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Builds the morning summary: what is due soon, the reminders about to go off, the next calendar dates and your current courses. */
public final class Digest {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("EEE MMM d, h:mm a", Locale.ENGLISH);

    private Digest() { }

    private static String clip(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1).trim() + "…";
    }

    /** @return the digest for a person, or null if there is nothing worth sending (so nobody is messaged for nothing) */
    public static EmbedBuilder build(long user, ZonedDateTime now) throws SQLException {
        ZoneId zone = now.getZone();
        LocalDate today = now.toLocalDate();
        List<String> lines = new ArrayList<>();

        StringBuilder due = new StringBuilder();
        for (DueItems.Item i : DueItems.open(user, null)) {
            ZonedDateTime at = Instant.ofEpochMilli(i.due).atZone(zone);
            long days = ChronoUnit.DAYS.between(today, at.toLocalDate());
            if (days > 7) break; // the list is soonest first
            String when = at.isBefore(now) ? "**overdue**, was " + at.format(TIME) : days == 0 ? "**today** " + at.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
                    : days == 1 ? "tomorrow " + at.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)) : at.format(TIME);
            due.append("**").append(i.code.replace("*", "\\*")).append("** ").append(clip(i.title, 50)).append(" · ").append(when).append('\n');
        }
        if (due.length() > 0) lines.add("Due\n" + due);

        StringBuilder reminders = new StringBuilder();
        long soon = now.toInstant().toEpochMilli() + 24L * 3600 * 1000;
        for (Reminders.Reminder r : Reminders.pending(user)) {
            if (r.due > soon) break;
            reminders.append("<t:").append(r.due / 1000).append(":t> ").append(clip(r.text, 80)).append('\n');
        }
        if (reminders.length() > 0) lines.add("Reminders in the next day\n" + reminders);

        StringBuilder dates = new StringBuilder();
        for (DateCatalog.Event e : DateCatalog.get().upcoming(today, Collections.<String>emptyList(), false, 40)) {
            if (ChronoUnit.DAYS.between(today, e.date) > 3) break;
            dates.append("**").append(e.date.format(DAY)).append("** ").append(clip(e.description, 110)).append('\n');
            if (dates.length() > 700) break;
        }
        if (dates.length() > 0) lines.add("On the academic calendar\n" + dates);

        if (lines.isEmpty()) return null;
        EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0xFEE75C)).setTitle("Good morning: " + today.format(DAY));
        StringBuilder body = new StringBuilder();
        for (String section : lines) {
            int nl = section.indexOf('\n');
            eb.addField(section.substring(0, nl), clip(section.substring(nl + 1), 1000), false);
        }
        eb.setDescription(body.toString().isEmpty() ? null : body.toString());
        eb.setFooter("!digest off stops this · !due and !reminders show everything", null);
        return eb;
    }
}

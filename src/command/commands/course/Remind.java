package command.commands.course;

import command.Command;
import command.ui.Buttons;
import courses.DateCatalog;
import courses.ReminderTime;
import courses.Reminders;
import courses.Reminders.Reminder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.awt.Color;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Reminders and the academic dates. {@code !remind 2h call mum} (or {@code friday 5pm}, {@code tomorrow}, {@code 2026-12-04})
 * sends a direct message when the time comes; {@code !remind here ...} posts in the channel instead. {@code !reminders}
 * lists what is waiting and {@code !dates} shows the next dates from the calendar, each with a button to be reminded.
 */
public final class Remind {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE MMM d", Locale.ENGLISH);

    private Remind() { }

    /** Keeps a reminder from pinging people or roles: the bot only ever pings the person who asked. */
    public static String defang(String text) {
        return text.replace("@", "@​");
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1).trim() + "…";
    }

    private static String stamp(long ms) {
        long s = ms / 1000;
        return "<t:" + s + ":f> (<t:" + s + ":R>)";
    }

    private static String usage() {
        return "Say `!remind <when> <what>`, for example `!remind 2h call mum`, `!remind friday 5pm hand in lab 3`, "
                + "`!remind tomorrow read chapter 4` or `!remind 2026-12-04 9:00 drop deadline`. `!remind here ...` posts "
                + "in this channel instead of a DM. See yours with `!reminders` and the next calendar dates with `!dates`. "
                + "Times are Guelph time.";
    }

    // ------------------------------------------------------------------ remind

    /** {@code !remind ...}. */
    public static class Create extends Command {

        public Create() {
            super("!remind", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().matches("^!remind(\\s+.*)?$");
        }

        @Override
        public void start(MessageReceivedEvent event) {
            long user = event.getAuthor().getIdLong();
            List<String> words = new ArrayList<>(Arrays.asList(event.getMessage().getContentRaw().trim().split("\\s+")));
            words.remove(0);
            try {
                if (words.isEmpty()) {
                    event.getChannel().sendMessage(usage()).queue();
                    return;
                }
                String first = words.get(0).toLowerCase(Locale.ROOT);
                if (first.equals("list")) {
                    list(event, user);
                    return;
                }
                if (first.equals("cancel")) {
                    cancel(event, user, words);
                    return;
                }
                if (first.equals("date")) {
                    fromCalendar(event, user, words);
                    return;
                }
                boolean here = first.equals("here");
                if (here) words.remove(0);
                ReminderTime.Result when = ReminderTime.parse(words, ZonedDateTime.now(ReminderTime.ZONE));
                if (when == null) {
                    event.getChannel().sendMessage("I could not read a time. " + usage()).queue();
                    return;
                }
                if (when.error != null) {
                    event.getChannel().sendMessage(when.error).queue();
                    return;
                }
                String text = String.join(" ", words.subList(when.used, words.size())).trim();
                if (text.isEmpty()) {
                    event.getChannel().sendMessage("What should I remind you about? `!remind 2h <what>`").queue();
                    return;
                }
                create(event, user, text, when.dueMs, here);
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }

        private void create(MessageReceivedEvent event, long user, String text, long due, boolean here) throws Exception {
            long id = Reminders.create(user, event.getChannel().getIdLong(), text, due, here);
            if (id < 0) {
                event.getChannel().sendMessage("You already have " + Reminders.MAX_PENDING + " reminders waiting. "
                        + "`!reminders` lists them and you can cancel some.").queue();
                return;
            }
            event.getChannel().sendMessage("Okay, " + (here ? "I will post here " : "I will message you ") + stamp(due) + ":\n> "
                            + defang(shorten(text, Reminders.MAX_TEXT)) + (here ? "" : "\n(Make sure you accept direct messages from this server.)"))
                    .setActionRow(Buttons.of(user, true, "!remind cancel " + id, "Cancel", ButtonStyle.DANGER)).queue();
        }

        void startList(MessageReceivedEvent event) {
            try {
                list(event, event.getAuthor().getIdLong());
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }

        private void list(MessageReceivedEvent event, long user) throws Exception {
            List<Reminder> pending = Reminders.pending(user);
            if (pending.isEmpty()) {
                event.getChannel().sendMessage("You have no reminders waiting. `!remind 2h call mum` makes one.").queue();
                return;
            }
            StringBuilder sb = new StringBuilder();
            List<ItemComponent> buttons = new ArrayList<>();
            for (Reminder r : pending) {
                String line = "**#" + r.id + "** <t:" + r.due / 1000 + ":f> (<t:" + r.due / 1000 + ":R>)" + (r.here ? " in the channel" : "")
                        + "\n" + defang(shorten(r.text, 120)) + "\n";
                if (sb.length() + line.length() > 3800) break;
                sb.append(line);
                if (buttons.size() < 5) buttons.add(Buttons.of(user, false, "!remind cancel " + r.id, "Cancel #" + r.id, ButtonStyle.SECONDARY));
            }
            EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0xFEE75C)).setTitle("Your reminders (" + pending.size() + ")")
                    .setDescription(sb.toString());
            event.getChannel().sendMessageEmbeds(eb.build()).setComponents(ActionRow.of(buttons)).queue();
        }

        private void cancel(MessageReceivedEvent event, long user, List<String> words) throws Exception {
            long id;
            try {
                id = Long.parseLong(words.get(1).replace("#", ""));
            } catch (RuntimeException e) {
                event.getChannel().sendMessage("Say `!remind cancel <number>`; `!reminders` shows the numbers.").queue();
                return;
            }
            event.getChannel().sendMessage(Reminders.cancel(user, id) ? "Cancelled reminder #" + id + "."
                    : "You have no waiting reminder #" + id + ".").queue();
        }

        /** {@code !remind date 2026-12-04 2}: the day before the second event on that date, at 9:00. */
        private void fromCalendar(MessageReceivedEvent event, long user, List<String> words) throws Exception {
            LocalDate day;
            int n = 1;
            try {
                day = LocalDate.parse(words.get(1));
                if (words.size() > 2) n = Integer.parseInt(words.get(2));
            } catch (RuntimeException e) {
                event.getChannel().sendMessage("Press a 🔔 button under `!dates` to be reminded of a date.").queue();
                return;
            }
            List<DateCatalog.Event> events = DateCatalog.get().on(day, true);
            if (n < 1 || n > events.size()) {
                event.getChannel().sendMessage("I do not have that date any more.").queue();
                return;
            }
            DateCatalog.Event e = events.get(n - 1);
            ZonedDateTime due = day.minusDays(1).atTime(ReminderTime.DEFAULT_TIME).atZone(ReminderTime.ZONE);
            ZonedDateTime now = ZonedDateTime.now(ReminderTime.ZONE);
            if (!due.isAfter(now.plus(ReminderTime.SOONEST))) due = day.atTime(ReminderTime.DEFAULT_TIME).atZone(ReminderTime.ZONE);
            if (!due.isAfter(now.plus(ReminderTime.SOONEST))) {
                event.getChannel().sendMessage("That date has already passed.").queue();
                return;
            }
            create(event, user, (due.toLocalDate().equals(day) ? "Today: " : "Tomorrow: ") + e.description, due.toInstant().toEpochMilli(), false);
        }
    }

    /** {@code !reminders}: the same list as {@code !remind list}. */
    public static class All extends Command {

        public All() {
            super("!reminders", true);
        }

        @Override
        public void start(MessageReceivedEvent event) {
            new Create().startList(event);
        }
    }

    // ------------------------------------------------------------------- dates

    /** {@code !dates [how many] [words]}. */
    public static class Dates extends Command {

        public Dates() {
            super("!dates", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().matches("^!dates(\\s+.*)?$");
        }

        @Override
        public void start(MessageReceivedEvent event) {
            long user = event.getAuthor().getIdLong();
            List<String> words = new ArrayList<>(Arrays.asList(event.getMessage().getContentRaw().trim().split("\\s+")));
            words.remove(0);
            int count = 8;
            if (!words.isEmpty() && words.get(0).matches("\\d{1,2}")) count = Math.max(1, Math.min(15, Integer.parseInt(words.remove(0))));
            boolean vet = words.remove("dvm");
            LocalDate today = ZonedDateTime.now(ReminderTime.ZONE).toLocalDate();
            List<DateCatalog.Event> found = DateCatalog.get().upcoming(today, words, vet, count);
            if (found.isEmpty()) {
                event.getChannel().sendMessage(words.isEmpty() ? "I have no upcoming dates. The calendar list may need refreshing."
                        : "No upcoming date mentions `" + String.join(" ", words) + "`. Try `!dates drop`, `!dates exam` or `!dates holiday`.").queue();
                return;
            }
            StringBuilder sb = new StringBuilder();
            List<ItemComponent> buttons = new ArrayList<>();
            for (DateCatalog.Event e : found) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, e.date);
                sb.append("**").append(e.date.format(DAY)).append("** · ").append(days == 0 ? "today" : days == 1 ? "tomorrow" : "in " + days + " days")
                        .append("\n").append(shorten(e.description, 170)).append("\n");
                if (buttons.size() < 5) {
                    int nth = DateCatalog.get().on(e.date, true).indexOf(e) + 1;
                    buttons.add(Buttons.of(user, false, "!remind date " + e.date + " " + nth,
                            "🔔 " + shorten(e.date.format(DAY) + " " + e.description, 60), ButtonStyle.SECONDARY));
                }
            }
            EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0x3498DB)).setTitle("Coming up on the academic calendar")
                    .setDescription(shorten(sb.toString(), 4000))
                    .setFooter("Press a bell to be reminded the day before · !dates drop, !dates exam, !dates 15", null);
            event.getChannel().sendMessageEmbeds(eb.build()).setComponents(ActionRow.of(buttons)).queue();
        }
    }
}

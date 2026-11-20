package command.commands.course;

import command.Command;
import command.ui.Buttons;
import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import courses.DueItems;
import courses.DueItems.Item;
import courses.ReminderTime;
import courses.Reminders;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.awt.Color;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What is due, per course. {@code !due add cis2500 lab3 friday 5pm} (put a name with spaces in quotes) adds an item and a
 * reminder the day before; {@code !due} lists what is coming, soonest first; {@code !due done 3} ticks one off and cancels its
 * reminder; {@code !due remove 3} deletes it. {@code !due cis2500} shows one course.
 */
public class Due extends Command {

    private static final Pattern TOKEN = Pattern.compile("\"([^\"]*)\"|(\\S+)");

    public Due() {
        super("!due", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().matches("^!due(\\s+.*)?$");
    }

    private static List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = TOKEN.matcher(text);
        while (m.find()) out.add(m.group(1) != null ? m.group(1) : m.group(2));
        return out;
    }

    private static String escape(String text) {
        return text.replace("*", "\\*");
    }

    private static String usage() {
        return "Say `!due add <course> <name> <when>`, for example `!due add cis2500 lab3 friday 5pm` (quotes for a name with "
                + "spaces: `\"final project\"`). `!due` lists what is coming, `!due done 3` ticks one off, `!due remove 3` deletes "
                + "it, `!due cis2500` shows one course. I remind you the day before.";
    }

    @Override
    public void start(MessageReceivedEvent event) {
        long user = event.getAuthor().getIdLong();
        List<String> words = tokens(event.getMessage().getContentRaw().trim());
        words.remove(0);
        try {
            if (words.isEmpty()) {
                list(event, user, null);
                return;
            }
            String first = words.get(0).toLowerCase();
            if (first.equals("add")) {
                add(event, user, words.subList(1, words.size()));
            } else if (first.equals("done") || first.equals("remove")) {
                close(event, user, words, first.equals("remove"));
            } else {
                Course c = CourseCatalog.get().find(String.join("", words));
                if (c == null) c = CourseCatalog.get().find(words.get(0));
                if (c == null) {
                    event.getChannel().sendMessage(usage()).queue();
                    return;
                }
                list(event, user, c.code);
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    private void add(MessageReceivedEvent event, long user, List<String> words) throws Exception {
        CourseCatalog catalog = CourseCatalog.get();
        Course c = words.isEmpty() ? null : catalog.find(words.get(0));
        int at = 1;
        if (c == null && words.size() > 1) {
            c = catalog.find(words.get(0) + words.get(1));
            at = 2;
        }
        if (c == null || words.size() < at + 2) {
            event.getChannel().sendMessage(usage()).queue();
            return;
        }
        String title = words.get(at);
        ZonedDateTime now = ZonedDateTime.now(ReminderTime.ZONE);
        ReminderTime.Result when = ReminderTime.parse(new ArrayList<>(words.subList(at + 1, words.size())), now);
        if (when == null) {
            event.getChannel().sendMessage("I could not read the time. " + usage()).queue();
            return;
        }
        if (when.error != null) {
            event.getChannel().sendMessage(when.error).queue();
            return;
        }
        // a reminder the day before, or an hour before if that is too late
        long dayBefore = when.dueMs - 24L * 3600 * 1000;
        long hourBefore = when.dueMs - 3600L * 1000;
        long remindAt = dayBefore > now.toInstant().toEpochMilli() + 60_000 ? dayBefore
                : hourBefore > now.toInstant().toEpochMilli() + 60_000 ? hourBefore : 0;
        long reminder = remindAt == 0 ? 0 : Reminders.create(user, event.getChannel().getIdLong(),
                "Due " + (remindAt == dayBefore ? "tomorrow" : "in an hour") + ": " + c.code + " " + title, remindAt, false);
        long id = DueItems.add(user, c.code, title, when.dueMs, Math.max(reminder, 0));
        if (id < 0) {
            if (reminder > 0) Reminders.cancel(user, reminder);
            event.getChannel().sendMessage("You already have " + DueItems.MAX_OPEN + " open items. `!due done <number>` some first.").queue();
            return;
        }
        event.getChannel().sendMessage("Added **#" + id + "** " + escape(c.code) + " " + " " + Remind.defang(title) + ", due <t:" + when.dueMs / 1000 + ":f> (<t:" + when.dueMs / 1000 + ":R>)."
                + (reminder > 0 ? " I will remind you " + (remindAt == dayBefore ? "the day before." : "an hour before.")
                : remindAt == 0 ? " It is too soon for a reminder." : reminder < 0 ? " You have too many reminders waiting, so I did not set one."
                : ""))
                .setActionRow(Buttons.of(user, false, "!due done " + id, "Done", ButtonStyle.SUCCESS)).queue();
    }

    private void close(MessageReceivedEvent event, long user, List<String> words, boolean delete) throws Exception {
        long id;
        try {
            id = Long.parseLong(words.get(1).replace("#", ""));
        } catch (RuntimeException e) {
            event.getChannel().sendMessage("Say `!due " + words.get(0) + " <number>`; `!due` shows the numbers.").queue();
            return;
        }
        Item item = DueItems.close(user, id, delete);
        event.getChannel().sendMessage(item == null ? "You have no open item #" + id + "."
                : (delete ? "Removed " : "Done: ") + escape(item.code) + " " + Remind.defang(item.title) + (delete ? "." : ". Nice work.")).queue();
    }

    private void list(MessageReceivedEvent event, long user, String code) throws Exception {
        List<Item> items = DueItems.open(user, code);
        if (items.isEmpty()) {
            event.getChannel().sendMessage(code == null ? "Nothing is due. `!due add cis2500 lab3 friday 5pm` adds something."
                    : "Nothing is due in " + escape(code) + ".").queue();
            return;
        }
        long now = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        List<ItemComponent> buttons = new ArrayList<>();
        for (Item i : items) {
            String line = "**#" + i.id + "** " + escape(i.code) + " " + Remind.defang(i.title) + " · <t:" + i.due / 1000 + ":f> (<t:" + i.due / 1000 + ":R>)"
                    + (i.due < now ? " **overdue**" : "") + "\n";
            if (sb.length() + line.length() > 3800) break;
            sb.append(line);
            if (buttons.size() < 5) buttons.add(Buttons.of(user, false, "!due done " + i.id, "Done #" + i.id, ButtonStyle.SECONDARY));
        }
        EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0xFEE75C)).setTitle("Due" + (code == null ? "" : " · " + code.replace("*", "\\*"))
                + " (" + items.size() + ")").setDescription(sb.toString()).setFooter("!due done <n> · !due remove <n> · !due add <course> <name> <when>", null);
        event.getChannel().sendMessageEmbeds(eb.build()).setComponents(ActionRow.of(buttons)).queue();
    }
}

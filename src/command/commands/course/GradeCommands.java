package command.commands.course;

import command.Command;
import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import courses.Grades;
import courses.Grades.Item;
import courses.Grades.Need;
import courses.Grades.Standing;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A private grade tracker. {@code !grade cis1300 lab1 10 85} records an item (weight 10%, mark 85%; leave the mark off
 * if it is not marked yet), {@code !grades cis1300} shows where you stand, and {@code !need cis1300 70} says what you
 * need on the rest to finish with 70%. Marks are only ever sent to you by direct message.
 */
public final class GradeCommands {

    private GradeCommands() { }

    private static String escape(String text) {
        return text.replace("*", "\\*");
    }

    private static String pct(double v) {
        return String.format(Locale.ROOT, "%.1f%%", v);
    }

    /** Sends an embed by direct message and says so in the channel; never posts marks in the channel. */
    static void privately(MessageReceivedEvent event, MessageEmbed embed, String inChannel) {
        MessageChannel channel = event.getChannel();
        event.getAuthor().openPrivateChannel().queue(
                dm -> dm.sendMessageEmbeds(embed).queue(ok -> { }, err -> noDm(channel, event)),
                err -> noDm(channel, event));
        channel.sendMessage(inChannel + " I sent the numbers to your DMs: marks are never posted here.").queue();
    }

    private static void noDm(MessageChannel channel, MessageReceivedEvent event) {
        channel.sendMessage("<@" + event.getAuthor().getIdLong() + "> I could not send you a DM, so I have not shown your marks. "
                + "Allow direct messages from this server and try again.").queue();
    }

    private static Double number(String text) {
        try {
            return Double.parseDouble(text.replace("%", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** @return the course named by the first one or two words, and how many words that took; null if none */
    private static Object[] course(List<String> words, int at) {
        CourseCatalog catalog = CourseCatalog.get();
        if (at < words.size()) {
            Course c = catalog.find(words.get(at));
            if (c != null) return new Object[]{c, 1};
            if (at + 1 < words.size()) {
                c = catalog.find(words.get(at) + words.get(at + 1));
                if (c != null) return new Object[]{c, 2};
            }
        }
        return null;
    }

    static EmbedBuilder standingEmbed(Course c, List<Item> items) {
        Standing s = Grades.standing(items);
        EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0x3498DB)).setTitle(escape(c.code) + " · " + c.title);
        StringBuilder table = new StringBuilder("```\n");
        for (Item i : items) {
            table.append(String.format(Locale.ROOT, "%-14s %5.1f%%  %s%n", i.name.length() > 14 ? i.name.substring(0, 14) : i.name,
                    i.weight, i.mark == null ? "not marked" : String.format(Locale.ROOT, "%.1f%%", i.mark)));
        }
        table.append("```");
        eb.setDescription(items.isEmpty() ? "No items yet. `!grade " + c.code.replace("*", "").toLowerCase() + " lab1 10 85` adds one."
                : table.toString());
        if (s.average != null) {
            eb.addField("Average on marked work", pct(s.average), true);
            eb.addField("Earned so far", String.format(Locale.ROOT, "%.1f of %.1f points", s.earned, s.gradedWeight), true);
            eb.addField("Still to mark", pct(s.remaining), true);
            eb.addField("Possible final grade", pct(s.worst) + " to " + pct(s.best), false);
        }
        if (Math.abs(s.listedWeight - 100) > 0.01 && !items.isEmpty()) {
            eb.setFooter("Your items add up to " + pct(s.listedWeight) + ", not 100%. Add the rest for a complete picture.", null);
        } else {
            eb.setFooter("Only you can see this · !need " + c.code.replace("*", "").toLowerCase() + " 70 shows what you need for 70%", null);
        }
        return eb;
    }

    // ------------------------------------------------------------------- !grade

    /** {@code !grade <course> <name> <weight> [mark]}, {@code !grade remove <course> <name>}, {@code !grade clear <course>}. */
    public static class Record extends Command {

        public Record() {
            super("!grade", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().matches("^!grade(\\s+.*)?$");
        }

        private static String usage() {
            return "Say `!grade <course> <name> <weight%> [mark%]`, for example `!grade cis1300 midterm 25 72`. Leave the mark off "
                    + "if it is not marked yet and add it later with the same command. `!grade remove cis1300 midterm`, "
                    + "`!grade clear cis1300`, then `!grades cis1300` and `!need cis1300 70`. Everything is private.";
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
                String first = words.get(0).toLowerCase();
                if (first.equals("remove") || first.equals("clear")) {
                    Object[] found = course(words, 1);
                    if (found == null) {
                        event.getChannel().sendMessage(usage()).queue();
                        return;
                    }
                    Course c = (Course) found[0];
                    int next = 1 + (Integer) found[1];
                    if (first.equals("clear")) {
                        event.getChannel().sendMessage("Cleared " + Grades.clear(user, c.code) + " items for " + escape(c.code) + ".").queue();
                    } else if (next >= words.size()) {
                        event.getChannel().sendMessage(usage()).queue();
                    } else {
                        event.getChannel().sendMessage(Grades.remove(user, c.code, words.get(next).toLowerCase())
                                ? "Removed." : "You have no item with that name in " + escape(c.code) + ".").queue();
                    }
                    return;
                }
                Object[] found = course(words, 0);
                if (found == null) {
                    event.getChannel().sendMessage("I do not know the course `" + words.get(0) + "`. " + usage()).queue();
                    return;
                }
                Course c = (Course) found[0];
                int at = (Integer) found[1];
                if (words.size() < at + 2) {
                    event.getChannel().sendMessage(usage()).queue();
                    return;
                }
                String name = words.get(at).toLowerCase();
                Double weight = number(words.get(at + 1));
                Double mark = null;
                if (words.size() > at + 2 && !words.get(at + 2).equalsIgnoreCase("none") && !words.get(at + 2).equals("-")) {
                    mark = number(words.get(at + 2));
                    if (mark == null) {
                        event.getChannel().sendMessage(usage()).queue();
                        return;
                    }
                }
                if (weight == null || weight <= 0 || weight > 100 || (mark != null && (mark < 0 || mark > 150))) {
                    event.getChannel().sendMessage("The weight is above 0 and up to 100, and a mark is 0 to 150 (percent).").queue();
                    return;
                }
                if (name.length() > 40) {
                    event.getChannel().sendMessage("Keep the item's name under 40 characters.").queue();
                    return;
                }
                try {
                    Grades.save(user, c.code, new Item(name, weight, mark));
                } catch (IllegalStateException e) {
                    event.getChannel().sendMessage("You already have " + Grades.MAX_ITEMS_PER_COURSE + " items in " + escape(c.code)
                            + ". `!grade remove` some first.").queue();
                    return;
                }
                privately(event, standingEmbed(c, Grades.items(user, c.code)).build(), "Saved " + name + " for " + escape(c.code) + ".");
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }

    // ------------------------------------------------------------------ !grades

    /** {@code !grades [course]}. */
    public static class Show extends Command {

        public Show() {
            super("!grades", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().matches("^!grades(\\s+.*)?$");
        }

        @Override
        public void start(MessageReceivedEvent event) {
            long user = event.getAuthor().getIdLong();
            List<String> words = new ArrayList<>(Arrays.asList(event.getMessage().getContentRaw().trim().split("\\s+")));
            words.remove(0);
            try {
                if (!words.isEmpty()) {
                    Object[] found = course(words, 0);
                    if (found == null) {
                        event.getChannel().sendMessage("I do not know the course `" + words.get(0) + "`.").queue();
                        return;
                    }
                    Course c = (Course) found[0];
                    privately(event, standingEmbed(c, Grades.items(user, c.code)).build(), "Here is " + escape(c.code) + ".");
                    return;
                }
                java.util.Set<String> mine = Grades.courses(user);
                if (mine.isEmpty()) {
                    event.getChannel().sendMessage("You have no marks recorded. `!grade cis1300 lab1 10 85` adds your first (weight 10%, mark 85%).").queue();
                    return;
                }
                EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0x3498DB)).setTitle("Your courses");
                StringBuilder sb = new StringBuilder();
                CourseCatalog catalog = CourseCatalog.get();
                for (String code : mine) {
                    Standing s = Grades.standing(Grades.items(user, code));
                    Course c = catalog.find(code);
                    sb.append("**").append(escape(code)).append("** ").append(c == null ? "" : c.title).append(": ")
                            .append(s.average == null ? "nothing marked yet" : pct(s.average) + " on " + pct(s.gradedWeight) + " of the grade").append('\n');
                }
                eb.setDescription(sb.toString()).setFooter("Only you can see this · !grades <course> for the detail", null);
                privately(event, eb.build(), "Here are your courses.");
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }

    // -------------------------------------------------------------------- !need

    /** {@code !need <course> <target%>}. */
    public static class NeedCommand extends Command {

        public NeedCommand() {
            super("!need", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().matches("^!need(\\s+.*)?$");
        }

        @Override
        public void start(MessageReceivedEvent event) {
            long user = event.getAuthor().getIdLong();
            List<String> words = new ArrayList<>(Arrays.asList(event.getMessage().getContentRaw().trim().split("\\s+")));
            words.remove(0);
            try {
                Object[] found = course(words, 0);
                Double target = found == null || words.size() <= (Integer) found[1] ? null : number(words.get((Integer) found[1]));
                if (found == null || target == null || target <= 0 || target > 100) {
                    event.getChannel().sendMessage("Say `!need <course> <target%>`, for example `!need cis1300 70` for what you need "
                            + "on the rest of the course to finish with 70%.").queue();
                    return;
                }
                Course c = (Course) found[0];
                List<Item> items = Grades.items(user, c.code);
                if (items.isEmpty()) {
                    event.getChannel().sendMessage("You have no marks for " + escape(c.code) + " yet. `!grade " + c.code.replace("*", "").toLowerCase()
                            + " lab1 10 85` adds one.").queue();
                    return;
                }
                Need need = Grades.need(items, target);
                Standing s = Grades.standing(items);
                String text;
                Color colour;
                switch (need.outcome) {
                    case SECURED:
                        text = "✅ You have already earned " + pct(s.earned) + ", so " + pct(target) + " is secured whatever happens next.";
                        colour = new Color(0x57F287);
                        break;
                    case NOTHING_LEFT:
                        text = "Everything is marked and you finish on " + pct(s.earned) + ", short of " + pct(target) + ".";
                        colour = new Color(0xED4245);
                        break;
                    case IMPOSSIBLE:
                        text = "❌ Even full marks on the remaining " + pct(need.remaining) + " gets you " + pct(s.best) + ", short of "
                                + pct(target) + " (it would take " + pct(need.average) + ").";
                        colour = new Color(0xED4245);
                        break;
                    default:
                        text = "To finish with **" + pct(target) + "** you need an average of **" + pct(need.average) + "** on the remaining "
                                + pct(need.remaining) + " of the grade.";
                        colour = need.average > 90 ? new Color(0xFEE75C) : new Color(0x3498DB);
                }
                if (Math.abs(s.listedWeight - 100) > 0.01) text += "\n(Your items add up to " + pct(s.listedWeight) + ": any weight you have not listed counts as still to come.)";
                privately(event, new EmbedBuilder().setColor(colour).setTitle(escape(c.code) + " · what you need")
                        .setDescription(text).setFooter("Only you can see this", null).build(), "Worked out for " + escape(c.code) + ".");
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }
}

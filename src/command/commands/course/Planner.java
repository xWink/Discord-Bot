package command.commands.course;

import command.Command;
import command.ui.Buttons;
import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import command.util.misc.Prerequisites;
import command.util.misc.Prerequisites.Status;
import courses.CompletedStore;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Course planning from the courses you have finished: {@code !completed} keeps the list, {@code !next} shows what
 * you can take now, and {@code !whynot <course>} says what is missing for one course. Restrictions (programs,
 * credit counts, permission) are shown but cannot be checked, so treat "ready" as "the course prerequisites are met".
 */
public final class Planner {

    private static final Pattern CODE = Pattern.compile("(?i)\\b([a-z]{2,5})\\s?\\*?\\s?(\\d{4})\\b");
    private static final int READY_MAX = 20;
    private static final int MAYBE_MAX = 8;
    private static final int FIELD_MAX = 1000;

    private Planner() { }

    /** @return the course codes named in some text, in order, written any way ({@code cis1300}, {@code CIS*1300}) */
    static List<String> codesIn(String text, List<String> unknown) {
        List<String> out = new ArrayList<>();
        Matcher m = CODE.matcher(text);
        while (m.find()) {
            Course c = CourseCatalog.get().find(m.group(1) + m.group(2));
            if (c == null) {
                if (unknown != null) unknown.add((m.group(1) + "*" + m.group(2)).toUpperCase());
            } else if (!out.contains(c.code)) {
                out.add(c.code);
            }
        }
        return out;
    }

    private static String escape(String text) {
        return text.replace("*", "\\*");
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1).trim() + "…";
    }

    private static String arguments(MessageReceivedEvent event, String key) {
        return event.getMessage().getContentRaw().trim().replaceFirst("(?i)^" + Pattern.quote(key) + "\\s*", "").trim();
    }

    // ---------------------------------------------------------------- completed

    /** {@code !completed [add|remove|list|clear] [courses]}. */
    public static class Completed extends Command {

        public Completed() {
            super("!completed", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().matches("^!completed(\\s+.*)?$");
        }

        @Override
        public void start(MessageReceivedEvent event) {
            long user = event.getAuthor().getIdLong();
            String args = arguments(event, "!completed");
            String[] words = args.split("\\s+", 2);
            String action = words[0].toLowerCase();
            String rest = words.length > 1 ? words[1] : "";
            try {
                switch (action) {
                    case "add":
                    case "remove": {
                        List<String> unknown = new ArrayList<>();
                        List<String> codes = codesIn(rest, unknown);
                        if (codes.isEmpty()) {
                            event.getChannel().sendMessage("Say `!completed " + action + " cis1300 math1200 ...`."
                                    + (unknown.isEmpty() ? "" : " I do not know " + escape(String.join(", ", unknown)) + ".")).queue();
                            return;
                        }
                        int changed = action.equals("add") ? CompletedStore.add(user, codes) : CompletedStore.remove(user, codes);
                        String note = unknown.isEmpty() ? "" : "\nNot in the catalogue: " + escape(String.join(", ", unknown)) + ".";
                        event.getChannel().sendMessage((action.equals("add") ? "Added " : "Removed ") + changed + " course"
                                + (changed == 1 ? "" : "s") + ". You have " + CompletedStore.list(user).size() + " listed."
                                + note + "\nTry `!next` to see what you can take now.")
                                .setActionRow(Buttons.of(user, false, "!next", "What can I take next?", ButtonStyle.PRIMARY))
                                .queue();
                        return;
                    }
                    case "clear":
                        event.getChannel().sendMessage("Cleared " + CompletedStore.clear(user) + " courses.").queue();
                        return;
                    case "":
                    case "list": {
                        Set<String> done = CompletedStore.list(user);
                        if (done.isEmpty()) {
                            event.getChannel().sendMessage("You have not listed any finished courses. Say `!completed add "
                                    + "cis1300 math1200` and I can show what you can take next.").queue();
                            return;
                        }
                        EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0x57F287))
                                .setTitle("Finished courses (" + done.size() + ")")
                                .setDescription(shorten(escape(String.join(", ", done)), 3800))
                                .setFooter("!completed add / remove / clear · !next shows what is open to you", null);
                        event.getChannel().sendMessageEmbeds(eb.build())
                                .setActionRow(Buttons.of(user, false, "!next", "What can I take next?", ButtonStyle.PRIMARY)).queue();
                        return;
                    }
                    default:
                        event.getChannel().sendMessage("Say `!completed add cis1300 ...`, `!completed remove cis1300`, "
                                + "`!completed list` or `!completed clear`.").queue();
                }
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }

    // --------------------------------------------------------------------- next

    /** {@code !next [department] [level] [grad]}. */
    public static class Next extends Command {

        public Next() {
            super("!next", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().matches("^!next(\\s+.*)?$");
        }

        @Override
        public void start(MessageReceivedEvent event) {
            long user = event.getAuthor().getIdLong();
            try {
                Set<String> finished = CompletedStore.list(user);
                if (finished.isEmpty()) {
                    event.getChannel().sendMessage("Tell me what you have finished first: `!completed add cis1300 math1200`. "
                            + "Then `!next` shows the courses whose prerequisites you meet.").queue();
                    return;
                }
                String department = null;
                int level = 0;
                boolean grad = false;
                for (String word : arguments(event, "!next").split("\\s+")) {
                    if (word.isEmpty()) continue;
                    if (word.equalsIgnoreCase("grad")) grad = true;
                    else if (word.matches("[1-6]000?s?|[1-6]")) level = word.charAt(0) - '0';
                    else if (!CourseCatalog.get().department(word).isEmpty()) department = word.toUpperCase();
                    else {
                        event.getChannel().sendMessage("I do not know `" + word + "`. Say `!next`, `!next cis`, `!next cis 3` "
                                + "(third-year courses) or `!next grad`.").queue();
                        return;
                    }
                }
                CourseCatalog catalog = CourseCatalog.get();
                Set<String> done = Prerequisites.closure(catalog, finished);
                List<Course> ready = new ArrayList<>();
                List<Course> maybe = new ArrayList<>();
                for (String dept : catalog.departments()) {
                    if (department != null && !dept.equalsIgnoreCase(department)) continue;
                    for (Course c : catalog.department(dept)) {
                        if (done.contains(c.code)) continue;
                        if ((c.source.equals("graduate")) != grad && department == null) continue;
                        if (level != 0 && c.level() != level) continue;
                        Prerequisites p = Prerequisites.of(c);
                        // with no department named, only courses that something you finished leads to, not every intro course
                        boolean unlocked = false;
                        for (String code : c.prerequisiteCodes()) if (done.contains(code)) unlocked = true;
                        if (department == null && !unlocked) continue;
                        Status s = p.check(done);
                        if (s == Status.MET) ready.add(c);
                        else if (s == Status.UNKNOWN && unlocked) maybe.add(c);
                    }
                }
                if (ready.isEmpty() && maybe.isEmpty()) {
                    event.getChannel().sendMessage("Nothing new opens up from what you have listed"
                            + (department != null ? " in " + department : "") + ". Try a department: `!next cis`, or add more "
                            + "with `!completed add`.").queue();
                    return;
                }
                EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0x3498DB))
                        .setTitle("What you can take next" + (department != null ? " · " + department : "")
                                + (level != 0 ? " · " + level + "000-level" : ""));
                if (!ready.isEmpty()) eb.addField("Prerequisites met (" + ready.size() + ")", lines(ready, READY_MAX, null, done), false);
                if (!maybe.isEmpty()) eb.addField("Maybe: part of the prerequisite I cannot check", lines(maybe, MAYBE_MAX, true, done), false);
                eb.setFooter("Restrictions (programs, credits, permission) are not checked. Press a course for its entry.", null);
                List<ItemComponent> buttons = new ArrayList<>();
                for (Course c : ready.isEmpty() ? maybe : ready) {
                    if (buttons.size() == 5) break;
                    buttons.add(Buttons.of(0, false, "!info " + c.code, c.code, ButtonStyle.SECONDARY));
                }
                event.getChannel().sendMessageEmbeds(eb.build()).setComponents(ActionRow.of(buttons)).queue();
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }

        private static String lines(List<Course> courses, int max, Boolean why, Set<String> done) {
            StringBuilder sb = new StringBuilder();
            int shown = 0;
            for (Course c : courses) {
                if (shown == max) break;
                String line = "**" + escape(c.code) + "** " + c.title + (c.offered.isEmpty() ? "" : " · " + c.offered) + "\n";
                if (sb.length() + line.length() > FIELD_MAX - 40) break;
                sb.append(line);
                shown++;
            }
            if (courses.size() > shown) sb.append("…and ").append(courses.size() - shown).append(" more. Narrow it down with `!next <dept> <level>`.");
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------- whynot

    /** {@code !whynot <course>}. */
    public static class WhyNot extends Command {

        public WhyNot() {
            super("!whynot", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().matches("^!whynot(\\s+.*)?$");
        }

        @Override
        public void start(MessageReceivedEvent event) {
            long user = event.getAuthor().getIdLong();
            String query = arguments(event, "!whynot");
            if (query.isEmpty()) {
                event.getChannel().sendMessage("Say `!whynot <course>`, for example `!whynot cis3750`. It compares the "
                        + "course's prerequisites with the courses you listed under `!completed`.").queue();
                return;
            }
            try {
                CourseCatalog catalog = CourseCatalog.get();
                Course c = catalog.find(query);
                if (c == null) {
                    List<Course> found = catalog.search(query, 1);
                    c = found.isEmpty() ? null : found.get(0);
                }
                if (c == null) {
                    event.getChannel().sendMessage("I could not find a course matching `" + query + "`.").queue();
                    return;
                }
                Set<String> finished = CompletedStore.list(user);
                Set<String> done = Prerequisites.closure(catalog, finished);
                Prerequisites p = Prerequisites.of(c);
                Status s = p.check(done);
                EmbedBuilder eb = new EmbedBuilder().setTitle(escape(c.code) + " · " + c.title);
                String missing = p.missing(done);
                if (done.contains(c.code)) {
                    eb.setColor(new Color(0x99AAB5)).setDescription("You already list this course as finished.");
                } else if (p.isEmpty()) {
                    eb.setColor(new Color(0x57F287)).setDescription("✅ This course has no prerequisites.");
                } else if (s == Status.MET) {
                    eb.setColor(new Color(0x57F287)).setDescription("✅ You meet the listed prerequisites.");
                } else if (s == Status.UNKNOWN) {
                    eb.setColor(new Color(0xFEE75C)).setDescription("❓ You meet every part I can check, but I cannot "
                            + "check all of it:\n" + shorten(escape(missing), FIELD_MAX));
                } else {
                    eb.setColor(new Color(0xED4245)).setDescription("❌ Not yet. You still need:\n"
                            + shorten(escape(missing), FIELD_MAX));
                }
                if (!p.isEmpty()) eb.addField("The calendar says", shorten(escape(c.prerequisites), FIELD_MAX), false);
                if (!c.restrictions.isEmpty() && !c.restrictions.equalsIgnoreCase("none")) {
                    eb.addField("Restrictions (not checked)", shorten(escape(c.restrictions), FIELD_MAX), false);
                }
                if (finished.isEmpty()) eb.setFooter("You have not listed any finished courses: `!completed add cis1300 ...`", null);
                List<ItemComponent> buttons = new ArrayList<>();
                for (String code : c.prerequisiteCodes()) {
                    if (buttons.size() < 4 && !done.contains(code)) buttons.add(Buttons.of(0, false, "!info " + code, code, ButtonStyle.SECONDARY));
                }
                buttons.add(Buttons.of(0, false, "!prereq " + c.code, "Prerequisite tree", ButtonStyle.SUCCESS));
                event.getChannel().sendMessageEmbeds(eb.build()).setComponents(ActionRow.of(buttons)).queue();
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }
}

/*
 * Made by Jeremy Thorne.
 * GitHub: https://github.com/jeremyt123
 *
 * Edited by Shawn Kaplan
 */
package command.commands.misc;

import command.Command;
import command.ui.Buttons;
import command.ui.Reveal;
import command.util.art.CourseArt;
import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code !info <course>}: the full entry for a course code ({@code cis1300}, {@code CIS*1300}) under an animated
 * banner, with buttons for its prerequisites, related courses, its prerequisite tree and its channel. Anything that is not a code is searched instead: words from a title
 * ({@code !info intro programming}), a code's start ({@code !info cis13}) or a whole department ({@code !info cis}).
 */
public class Info extends Command {

    /** Discord shows at most 1024 characters in one embed field. */
    static final int FIELD_MAX = 1000;
    private static final int SUGGESTIONS = 5;
    private static final int DEPARTMENT_LIST = 20;

    public Info() {
        super("!info", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().matches("^" + getKey() + "(\\s+.*)?$");
    }

    @Override
    public void start(final MessageReceivedEvent event) {
        String query = event.getMessage().getContentRaw().trim().replaceFirst("(?i)^!info\\s*", "").trim();
        MessageChannel channel = event.getChannel();
        if (query.isEmpty()) {
            channel.sendMessage(usage()).queue();
            return;
        }
        CourseCatalog catalog = CourseCatalog.get();

        Course course = catalog.find(query);
        if (course != null) {
            show(channel, course);
            return;
        }
        List<Course> department = catalog.department(query);
        if (!department.isEmpty()) {
            channel.sendMessageEmbeds(departmentEmbed(query, department).build()).queue();
            return;
        }
        List<Course> found = catalog.search(query, SUGGESTIONS);
        if (found.isEmpty()) {
            channel.sendMessage("I could not find a course matching `" + query + "`. Try a code like `cis1300`, a few "
                    + "words from the title, or a department like `cis`.").queue();
            return;
        }
        if (found.size() == 1) {
            show(channel, found.get(0));
            return;
        }
        EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0x99AAB5)).setTitle("Courses matching \"" + shorten(query, 60) + "\"");
        StringBuilder list = new StringBuilder();
        List<ItemComponent> buttons = new ArrayList<>();
        for (Course c : found) {
            list.append("**").append(escape(c.code)).append("** ").append(c.title).append('\n');
            buttons.add(Buttons.of(0, false, "!info " + c.code, shorten(c.code + " " + c.title, 78), ButtonStyle.SECONDARY));
        }
        eb.setDescription(list.toString());
        eb.setFooter("Press one for the full entry", null);
        channel.sendMessageEmbeds(eb.build()).setComponents(rows(buttons)).queue();
    }

    /** Sends a course's entry with its banner animating above it; without the picture if it cannot be drawn. */
    private static void show(MessageChannel channel, Course course) {
        Reveal.async(() -> CourseArt.banner(course),
                gif -> channel.sendFiles(FileUpload.fromData(gif, "course.gif"))
                        .setEmbeds(courseEmbed(course).setImage("attachment://course.gif").build())
                        .setComponents(courseButtons(course)).queue(),
                () -> channel.sendMessageEmbeds(courseEmbed(course).build()).setComponents(courseButtons(course)).queue());
    }

    private static String usage() {
        return "Say `!info <course>`, for example `!info cis1300`. You can also search: `!info intro programming` finds "
                + "courses by title, and `!info cis` lists a department.";
    }

    /** The whole entry for one course. */
    static EmbedBuilder courseEmbed(Course c) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(levelColour(c.level()));
        eb.setTitle(escape(c.code) + " · " + c.title);
        List<String> facts = new ArrayList<>();
        if (!c.offered.isEmpty()) facts.add("**" + c.offered + "**");
        if (!c.hours.isEmpty()) facts.add(c.hours);
        if (!c.credits.isEmpty()) facts.add(c.credits + " credits");
        if (!facts.isEmpty()) eb.setDescription(String.join(" · ", facts));
        eb.addField("About", shorten(c.description.isEmpty() ? "No description." : c.description, FIELD_MAX), false);
        String pre = clean(c.prerequisites, "Prerequisite(s): ");
        eb.addField("Prerequisites", pre.isEmpty() ? "None" : shorten(escape(pre), FIELD_MAX), false);
        String co = clean(c.corequisites, "Co-requisite(s): ");
        if (!co.isEmpty()) eb.addField("Co-requisites (take at the same time)", shorten(escape(co), FIELD_MAX), false);
        String equates = clean(c.equates, "Equate(s): ");
        if (!equates.isEmpty()) eb.addField("Counts as the same course", shorten(escape(equates), FIELD_MAX), false);
        String restrictions = clean(c.restrictions, "Restriction(s): ");
        if (!restrictions.isEmpty()) eb.addField("Restrictions", shorten(escape(restrictions), FIELD_MAX), false);
        if (!c.offering.isEmpty()) eb.addField("How it is offered", shorten(c.offering, FIELD_MAX), false);
        List<String> foot = new ArrayList<>();
        foot.add(c.source.equals("graduate") ? "Graduate" : c.level() + "000-level");
        if (!c.unit.isEmpty()) foot.add(c.unit);
        if (!c.locations.isEmpty()) foot.add(c.locations);
        eb.setFooter(String.join(" · ", foot), null);
        return eb;
    }

    /** One button per prerequisite, one per related course, then join, tree and department buttons. */
    static List<ActionRow> courseButtons(Course c) {
        CourseCatalog catalog = CourseCatalog.get();
        List<ItemComponent> prereqs = new ArrayList<>();
        for (String code : c.prerequisiteCodes()) {
            if (prereqs.size() == 5) break;
            Course p = catalog.find(code);
            prereqs.add(Buttons.of(0, false, "!info " + code, shorten("\u2B06 " + code + (p == null ? "" : " " + p.title), 78),
                    ButtonStyle.SECONDARY));
        }
        List<ItemComponent> related = new ArrayList<>();
        for (String code : c.equateCodes()) {
            if (related.size() < 3) related.add(Buttons.of(0, false, "!info " + code, "= " + code, ButtonStyle.SECONDARY));
        }
        for (String code : c.corequisiteCodes()) {
            if (related.size() < 5) related.add(Buttons.of(0, false, "!info " + code, "+ " + code, ButtonStyle.SECONDARY));
        }
        List<ItemComponent> actions = new ArrayList<>();
        actions.add(Buttons.of(0, false, "!join " + c.department + c.number, "Join the " + c.department + c.number + " channel",
                ButtonStyle.PRIMARY));
        if (!c.prerequisiteCodes().isEmpty()) {
            actions.add(Buttons.of(0, false, "!prereq " + c.code, "Prerequisite tree", ButtonStyle.SUCCESS));
        }
        actions.add(Buttons.of(0, false, "!info " + c.department, "More " + c.department + " courses", ButtonStyle.SECONDARY));
        List<ActionRow> out = new ArrayList<>();
        if (!prereqs.isEmpty()) out.add(ActionRow.of(prereqs));
        if (!related.isEmpty()) out.add(ActionRow.of(related));
        out.add(ActionRow.of(actions));
        return out;
    }

    private static EmbedBuilder departmentEmbed(String query, List<Course> courses) {
        EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0x99AAB5));
        String dept = courses.get(0).department;
        eb.setTitle(dept + " courses (" + courses.size() + ")");
        StringBuilder list = new StringBuilder();
        int shown = Math.min(DEPARTMENT_LIST, courses.size());
        for (int i = 0; i < shown; i++) {
            Course c = courses.get(i);
            list.append("**").append(escape(c.code)).append("** ").append(c.title).append('\n');
        }
        if (courses.size() > shown) {
            list.append("…and ").append(courses.size() - shown).append(" more. Narrow it down: `!info ")
                    .append(dept.toLowerCase()).append(" <word>` or a code like `!info ").append(dept.toLowerCase())
                    .append(courses.get(shown).number).append("`.");
        }
        eb.setDescription(list.toString());
        return eb;
    }

    private static List<ActionRow> rows(List<ItemComponent> buttons) {
        List<ActionRow> rows = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i += 5) rows.add(ActionRow.of(buttons.subList(i, Math.min(buttons.size(), i + 5))));
        return rows;
    }

    private static Color levelColour(int level) {
        switch (level) {
            case 1: return new Color(0x57F287);
            case 2: return new Color(0x3498DB);
            case 3: return new Color(0xE67E22);
            case 4: return new Color(0xED4245);
            default: return new Color(0x9B59B6);
        }
    }

    /** Removes the catalogue's label and the word NONE. */
    static String clean(String text, String label) {
        String t = text.replace(label, "").trim();
        return t.equalsIgnoreCase("NONE") ? "" : t;
    }

    /** Stops the asterisk in a course code from turning the text into italics. */
    static String escape(String text) {
        return text.replace("*", "\\*");
    }

    static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1).trim() + "…";
    }
}

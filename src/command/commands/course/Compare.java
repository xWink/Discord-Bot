package command.commands.course;

import command.Command;
import command.commands.misc.Info;
import command.ui.Buttons;
import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import command.util.misc.Prerequisites;
import courses.Ratings;
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

/**
 * {@code !compare cis2500 cis2520} puts two or three courses side by side: when they run, hours, credits, what they need,
 * what students say, and where you stand on each.
 */
public class Compare extends Command {

    public Compare() {
        super("!compare", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().matches("^!compare(\\s+.*)?$");
    }

    private static String escape(String text) {
        return text.replace("*", "\\*");
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1).trim() + "…";
    }

    /** Where you stand on a course, in a few words. */
    static String status(Course c, Set<String> finished, Set<String> taking, Set<String> closure) {
        if (finished.contains(c.code) || closure.contains(c.code)) return "✅ finished";
        if (taking.contains(c.code)) return "📚 taking now";
        Prerequisites p = Prerequisites.of(c);
        switch (p.check(closure)) {
            case MET: return p.isEmpty() ? "🟦 no prerequisites" : "🟦 ready to take";
            case UNKNOWN: return "🟨 maybe: part cannot be checked";
            default: return "⬜ not yet: " + shorten(escape(p.missing(closure)), 90);
        }
    }

    /** The comparison as an embed; separate from the command so a test can check it without Discord. */
    static EmbedBuilder embed(List<Course> courses, Set<String> finished, Set<String> taking) {
        Set<String> closure = Prerequisites.closure(CourseCatalog.get(), new LinkedHashSet<String>() {{
            addAll(finished);
        }});
        EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0x3498DB));
        List<String> codes = new ArrayList<>();
        for (Course c : courses) codes.add(escape(c.code));
        eb.setTitle(String.join(" vs ", codes));
        for (Course c : courses) {
            StringBuilder v = new StringBuilder();
            v.append("**Runs** ").append(c.offered.isEmpty() ? "not stated" : c.offered).append('\n');
            v.append("**Hours** ").append(c.hours.isEmpty() ? "not stated" : c.hours).append('\n');
            v.append("**Credits** ").append(c.credits).append(" · ").append(c.source.equals("graduate") ? "graduate" : c.level() + "000-level").append('\n');
            String pre = Info.clean(c.prerequisites, "Prerequisite(s): ");
            v.append("**Needs** ").append(pre.isEmpty() ? "nothing" : shorten(escape(pre), 140)).append('\n');
            Ratings.Summary r = Info.ratingsFor(c.code);
            v.append("**Students** ").append(r == null || !r.enoughToShow() ? "not enough ratings" : "difficulty "
                    + String.format("%.1f", r.difficulty) + ", workload " + String.format("%.1f", r.workload) + " (" + r.count + ")").append('\n');
            v.append("**You** ").append(status(c, finished, taking, closure));
            eb.addField(escape(c.code) + " " + shorten(c.title, 40), shorten(v.toString(), 1000), true);
        }
        eb.setFooter("Press a course for its entry", null);
        return eb;
    }

    @Override
    public void start(MessageReceivedEvent event) {
        long user = event.getAuthor().getIdLong();
        String args = event.getMessage().getContentRaw().trim().replaceFirst("(?i)^!compare\\s*", "").trim();
        try {
            List<String> unknown = new ArrayList<>();
            List<String> codes = Planner.codesIn(args, unknown);
            if (codes.size() < 2) {
                event.getChannel().sendMessage("Say `!compare <course> <course>`, for example `!compare cis2500 cis2520` (up to three)."
                        + (unknown.isEmpty() ? "" : " I do not know " + escape(String.join(", ", unknown)) + ".")).queue();
                return;
            }
            if (codes.size() > 3) codes = codes.subList(0, 3);
            CourseCatalog catalog = CourseCatalog.get();
            List<Course> courses = new ArrayList<>();
            for (String code : codes) courses.add(catalog.find(code));
            Set<String>[] mine = PlanCommand.source.apply(user);
            EmbedBuilder eb = embed(courses, mine[0], mine[1]);
            List<ItemComponent> buttons = new ArrayList<>();
            for (Course c : courses) buttons.add(Buttons.of(0, false, "!info " + c.code, c.code, ButtonStyle.SECONDARY));
            event.getChannel().sendMessageEmbeds(eb.build()).setComponents(ActionRow.of(buttons)).queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

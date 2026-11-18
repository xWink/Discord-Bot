package command.commands.misc;

import command.Command;
import command.ui.Reveal;
import command.util.art.CourseArt;
import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code !prereq <course>}: draws what a course needs, and what those courses need in turn, as boxes joined by
 * arrows that build up from the first-year courses to the one asked about.
 */
public class Prereq extends Command {

    public Prereq() {
        super("!prereq", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().matches("^" + getKey() + "(\\s+.*)?$");
    }

    @Override
    public void start(MessageReceivedEvent event) {
        String query = event.getMessage().getContentRaw().trim().replaceFirst("(?i)^!prereq\\s*", "").trim();
        if (query.isEmpty()) {
            event.getChannel().sendMessage("Say `!prereq <course>`, for example `!prereq cis3750`. It draws what the "
                    + "course needs and what those courses need in turn.").queue();
            return;
        }
        CourseCatalog catalog = CourseCatalog.get();
        Course course = catalog.find(query);
        if (course == null) {
            List<Course> found = catalog.search(query, 1);
            course = found.isEmpty() ? null : found.get(0);
        }
        if (course == null) {
            event.getChannel().sendMessage("I could not find a course matching `" + query + "`.").queue();
            return;
        }
        if (CourseArt.prerequisiteCount(catalog, course) == 0) {
            event.getChannel().sendMessage(course.code + " " + course.title + " has no prerequisite courses"
                    + (course.prerequisites.isEmpty() ? "." : ": " + course.prerequisites.replace("*", "\\*"))).queue();
            return;
        }
        Course target = course;
        String text = "**" + target.code.replace("*", "\\*") + "** needs: " + target.prerequisites.replace("*", "\\*");
        Reveal.render(event.getChannel(), "prereq.gif", "Tracing the prerequisites\u2026",
                () -> CourseArt.prerequisiteTree(catalog, target), text, new ArrayList<>(), 1500);
    }
}

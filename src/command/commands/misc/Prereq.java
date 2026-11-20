package command.commands.misc;

import command.Command;
import command.ui.Buttons;
import command.ui.Reveal;
import command.util.art.CourseArt;
import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import command.util.misc.Prerequisites;
import courses.CompletedStore;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code !prereq <course>}: draws what a course needs, and what those courses need in turn, as boxes joined by
 * arrows that build up from the first-year courses to the one asked about.
 */
public class Prereq extends Command {

    /** Where a person's finished courses come from; tests that have no database replace it. */
    static java.util.function.Function<Long, java.util.Set<String>> completedSource = user -> {
        try {
            return CompletedStore.list(user);
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(e);
        }
    };

    /** For tests: use this instead of the database. */
    public static void useCompleted(java.util.function.Function<Long, java.util.Set<String>> source) {
        completedSource = source;
    }

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
        Course course = command.commands.course.CourseLookup.resolve(event.getChannel(), query, "!prereq");
        if (course == null) return;
        if (CourseArt.prerequisiteCount(catalog, course) == 0) {
            event.getChannel().sendMessage(course.code + " " + course.title + " has no prerequisite courses"
                    + (course.prerequisites.isEmpty() ? "." : ": " + course.prerequisites.replace("*", "\\*"))).queue();
            return;
        }
        Course target = course;
        // what this person has finished, if they have told me; no list (or no database) just means a plain tree
        java.util.Set<String> done = java.util.Collections.emptySet();
        try {
            done = Prerequisites.closure(catalog, completedSource.apply(event.getAuthor().getIdLong()));
        } catch (Exception e) {
            // a plain tree is fine
        }
        java.util.Set<String> finished = done;
        CourseArt.Progress progress = CourseArt.progress(catalog, target, finished);
        StringBuilder text = new StringBuilder("**" + target.code.replace("*", "\\*") + "** needs: "
                + target.prerequisites.replace("*", "\\*"));
        List<ItemComponent> buttons = new ArrayList<>();
        if (finished.isEmpty()) {
            text.append("\nAdd the courses you have finished with `!completed add cis1300 ...` and this tree shows your progress.");
        } else {
            text.append("\n").append(progress.finished).append(" of ").append(progress.needed)
                    .append(" prerequisite courses finished. ");
            if (progress.target == CourseArt.State.READY) text.append("**You can take it now.**");
            else if (progress.target == CourseArt.State.MAYBE) text.append("You meet everything I can check.");
            else if (!progress.nextUp.isEmpty()) {
                List<String> names = new ArrayList<>();
                for (Course c : progress.nextUp.subList(0, Math.min(4, progress.nextUp.size()))) names.add(c.code.replace("*", "\\*"));
                text.append("Next up: ").append(String.join(", ", names)).append(".");
            }
            for (Course c : progress.nextUp) {
                if (buttons.size() < 5) buttons.add(Buttons.of(0, false, "!info " + c.code, "\u25B6 " + c.code, ButtonStyle.PRIMARY));
            }
        }
        Reveal.render(event.getChannel(), "prereq.gif", "Tracing the prerequisites\u2026",
                () -> CourseArt.prerequisiteTree(catalog, target, finished), text.toString(), buttons, 1500);
    }
}

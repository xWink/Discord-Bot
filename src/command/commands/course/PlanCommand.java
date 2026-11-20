package command.commands.course;

import command.Command;
import command.ui.Buttons;
import command.ui.Reveal;
import command.util.art.CourseArt;
import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import command.util.misc.Prerequisites;
import courses.CompletedStore;
import courses.DegreePlan;
import courses.DegreePlan.Placement;
import courses.DegreePlan.Plan;
import courses.DegreePlan.TermPlan;
import courses.TakingStore;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@code !plan cis3750} lays out the earliest semester-by-semester route to a course from what you have finished (and what
 * you are taking now, assumed finished by the end of this term). {@code !plan cis3750 3} limits it to three courses a term.
 * Courses go in terms they are offered; anything that cannot be checked, like a credit count, is listed as a note.
 */
public class PlanCommand extends Command {

    public PlanCommand() {
        super("!plan", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().matches("^!plan(\\s+.*)?$");
    }

    private static String escape(String text) {
        return text.replace("*", "\\*");
    }

    /** Where a person's finished and current courses come from; tests that have no database replace it. */
    static java.util.function.Function<Long, Set<String>[]> source = user -> {
        try {
            @SuppressWarnings("unchecked") Set<String>[] both = new Set[]{CompletedStore.list(user), TakingStore.list(user)};
            return both;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(e);
        }
    };

    /** For tests: finished and current courses, instead of the database. */
    public static void useSource(java.util.function.Function<Long, Set<String>[]> replacement) {
        source = replacement;
    }

    /** The plan as text, which is the whole answer when animations are off. */
    static String describe(Plan plan, boolean assumedTaking, Set<String> taking) {
        StringBuilder sb = new StringBuilder();
        if (plan.alreadyDone) return "You have already finished **" + escape(plan.target.code) + "**.";
        for (TermPlan tp : plan.terms) {
            sb.append("**").append(tp.term.label()).append("**: ");
            if (tp.courses.isEmpty()) {
                sb.append("nothing fits this term\n");
                continue;
            }
            List<String> names = new ArrayList<>();
            for (Placement pl : tp.courses) names.add(escape(pl.course.code) + (pl.termUncertain ? "?" : ""));
            sb.append(String.join(", ", names)).append('\n');
        }
        if (plan.complete) {
            sb.append("\nEarliest finish: **").append(plan.finish().label()).append("** (").append(plan.terms.size())
                    .append(plan.terms.size() == 1 ? " term" : " terms").append(").");
        }
        if (assumedTaking && !taking.isEmpty()) {
            List<String> t = new ArrayList<>();
            for (String c : taking) t.add(escape(c));
            sb.append("\nAssuming you finish ").append(String.join(", ", t)).append(" this term.");
        }
        for (String note : plan.notes) sb.append("\n• ").append(escape(note));
        return sb.toString();
    }

    @Override
    public void start(MessageReceivedEvent event) {
        long user = event.getAuthor().getIdLong();
        List<String> words = new ArrayList<>(java.util.Arrays.asList(event.getMessage().getContentRaw().trim().split("\\s+")));
        words.remove(0);
        if (words.isEmpty()) {
            event.getChannel().sendMessage("Say `!plan <course>`, for example `!plan cis3750`, or `!plan cis3750 3` for at most three "
                    + "courses a term. It plans the earliest route from the courses you have finished (`!completed add`) and are taking "
                    + "now (`!taking add`).").queue();
            return;
        }
        int perTerm = DegreePlan.DEFAULT_PER_TERM;
        String last = words.get(words.size() - 1);
        if (words.size() > 1 && last.matches("[1-9]")) {
            perTerm = Integer.parseInt(last);
            words.remove(words.size() - 1);
        }
        try {
            Course target = CourseLookup.resolve(event.getChannel(), String.join(" ", words), "!plan");
            if (target == null) return;
            CourseCatalog catalog = CourseCatalog.get();
            Set<String>[] mine = source.apply(user);
            Set<String> finished = mine[0];
            Set<String> taking = mine[1];
            Set<String> assumed = new LinkedHashSet<>(finished);
            assumed.addAll(taking); // what they are taking now is assumed finished by the end of the term
            Set<String> done = Prerequisites.closure(catalog, assumed);
            Plan plan = DegreePlan.plan(catalog, target, done, DegreePlan.Term.after(LocalDate.now()), perTerm);
            StringBuilder text = new StringBuilder(describe(plan, true, taking));
            if (finished.isEmpty() && taking.isEmpty() && !plan.alreadyDone) {
                text.append("\n(You have not listed any courses, so this starts from nothing. `!completed add cis1300 ...` and `!taking add ...` "
                        + "make it yours.)");
            }
            if (plan.alreadyDone || plan.terms.isEmpty()) {
                event.getChannel().sendMessage(text.toString()).queue();
                return;
            }
            List<ItemComponent> buttons = new ArrayList<>();
            for (Placement pl : plan.terms.get(0).courses) {
                if (buttons.size() < 5) buttons.add(Buttons.of(0, false, "!info " + pl.course.code, "▶ " + pl.course.code, ButtonStyle.PRIMARY));
            }
            int shown = perTerm;
            Reveal.render(event.getChannel(), "plan.gif", "Drawing your route…", () -> CourseArt.roadmap(plan, shown), text.toString(), buttons, 1500);
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

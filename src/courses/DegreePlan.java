package courses;

import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import command.util.misc.Prerequisites;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A semester-by-semester route to a course. It works out which courses would still have to be taken (choosing the shortest
 * way through each "or"), then fills the coming terms: a course goes in the first term in which everything it needs is already
 * finished and the catalogue says it is offered, longest chains first, with a limit on courses per term. Credit counts and
 * other things that cannot be checked are listed as notes, and a course whose term the catalogue does not give is placed
 * anywhere and marked uncertain.
 */
public final class DegreePlan {

    public static final int DEFAULT_PER_TERM = 5;
    public static final int MAX_TERMS = 16;

    private DegreePlan() { }

    public enum Season {
        WINTER("Winter"), SUMMER("Summer"), FALL("Fall");

        public final String label;

        Season(String label) {
            this.label = label;
        }
    }

    /** A term of the year, for example Fall 2026. */
    public static final class Term {
        public final Season season;
        public final int year;

        public Term(Season season, int year) {
            this.season = season;
            this.year = year;
        }

        /** Winter is January to April, summer May to August and fall September to December. */
        public Term next() {
            switch (season) {
                case WINTER: return new Term(Season.SUMMER, year);
                case SUMMER: return new Term(Season.FALL, year);
                default: return new Term(Season.WINTER, year + 1);
            }
        }

        /** @return the term after the one {@code day} falls in */
        public static Term after(LocalDate day) {
            int m = day.getMonthValue();
            Term current = m <= 4 ? new Term(Season.WINTER, day.getYear()) : m <= 8 ? new Term(Season.SUMMER, day.getYear())
                    : new Term(Season.FALL, day.getYear());
            return current.next();
        }

        public String label() {
            return season.label + " " + year;
        }

        @Override
        public String toString() {
            return label();
        }
    }

    /** What the catalogue says about when a course runs. */
    public enum Offering { YES, NO, UNKNOWN }

    public static Offering offered(Course c, Season season) {
        String t = c.offered.toLowerCase(Locale.ROOT);
        if (t.isEmpty() || t.contains("unspecified")) return Offering.UNKNOWN;
        return t.contains(season.label.toLowerCase(Locale.ROOT)) ? Offering.YES : Offering.NO;
    }

    /** One planned course. */
    public static final class Placement {
        public final Course course;
        /** The courses in the plan it needs, which are all in earlier terms. */
        public final List<String> needs = new ArrayList<>();
        /** True if the catalogue does not say when it is offered, so the term is a guess. */
        public boolean termUncertain;

        Placement(Course course) {
            this.course = course;
        }
    }

    /** One term of the plan. */
    public static final class TermPlan {
        public final Term term;
        public final List<Placement> courses = new ArrayList<>();

        TermPlan(Term term) {
            this.term = term;
        }
    }

    /** The whole plan. */
    public static final class Plan {
        public final Course target;
        public final List<TermPlan> terms = new ArrayList<>();
        /** Things the plan could not check or assumed. */
        public final List<String> notes = new ArrayList<>();
        /** False if the target could not be placed (for example it is never offered in a term that fits). */
        public boolean complete;
        /** True if the person has already finished the target. */
        public boolean alreadyDone;

        Plan(Course target) {
            this.target = target;
        }

        /** @return the term the target is in, or null */
        public Term finish() {
            return complete && !terms.isEmpty() ? terms.get(terms.size() - 1).term : null;
        }
    }

    /**
     * @param done finished courses, with equivalent courses already added (see {@link Prerequisites#closure})
     * @param start the first term to plan
     * @param perTerm the most courses in one term
     */
    public static Plan plan(CourseCatalog catalog, Course target, Set<String> done, Term start, int perTerm) {
        Plan plan = new Plan(target);
        if (done.contains(target.code)) {
            plan.alreadyDone = true;
            plan.complete = true;
            return plan;
        }
        // what has to be taken: the target and, below it, what each needs (one way through each "or")
        Map<String, Course> needed = new LinkedHashMap<>();
        Map<String, List<String>> deps = new HashMap<>();
        List<Course> queue = new ArrayList<>();
        queue.add(target);
        needed.put(target.code, target);
        Set<String> notInCatalogue = new LinkedHashSet<>();
        Set<String> unchecked = new LinkedHashSet<>();
        for (int i = 0; i < queue.size() && needed.size() < 40; i++) {
            Course c = queue.get(i);
            Prerequisites p = Prerequisites.of(c);
            List<String> own = new ArrayList<>();
            for (String code : p.needed(done)) {
                Course d = catalog.find(code);
                if (d == null) {
                    notInCatalogue.add(code);
                    continue;
                }
                own.add(code);
                if (!needed.containsKey(code)) {
                    needed.put(code, d);
                    queue.add(d);
                }
            }
            deps.put(c.code, own);
            for (String u : p.unchecked()) unchecked.add(c.code + ": " + u + " (not checked)");
        }
        plan.notes.addAll(unchecked);
        for (String code : notInCatalogue) plan.notes.add(code + " is not in the catalogue, so I left it out.");
        if (!deps.isEmpty() && cyclic(deps)) {
            plan.notes.add("The prerequisites loop back on themselves, so this route may be incomplete.");
        }

        // how long a chain each course heads, so the ones others wait on go first
        Map<String, Integer> height = new HashMap<>();
        for (String code : needed.keySet()) height.put(code, 1);
        for (int pass = 0; pass < needed.size(); pass++) {
            boolean changed = false;
            for (Map.Entry<String, List<String>> e : deps.entrySet()) {
                for (String d : e.getValue()) {
                    int h = height.get(e.getKey()) + 1;
                    if (height.get(d) < h && h <= needed.size()) {
                        height.put(d, h);
                        changed = true;
                    }
                }
            }
            if (!changed) break;
        }

        Set<String> finished = new LinkedHashSet<>();
        Set<String> placed = new LinkedHashSet<>();
        Term term = start;
        int limit = Math.max(1, perTerm);
        for (int t = 0; t < MAX_TERMS && !placed.contains(target.code); t++, term = term.next()) {
            List<Course> ready = new ArrayList<>();
            for (Course c : needed.values()) {
                if (placed.contains(c.code)) continue;
                boolean depsDone = true;
                for (String d : deps.getOrDefault(c.code, new ArrayList<String>())) if (!finished.contains(d)) depsDone = false;
                if (depsDone && offered(c, term.season) != Offering.NO) ready.add(c);
            }
            ready.sort((a, b) -> {
                int d = Integer.compare(height.get(b.code), height.get(a.code));
                return d != 0 ? d : a.code.compareTo(b.code);
            });
            TermPlan tp = new TermPlan(term);
            for (Course c : ready) {
                if (tp.courses.size() >= limit) break;
                Placement pl = new Placement(c);
                pl.needs.addAll(deps.getOrDefault(c.code, new ArrayList<String>()));
                pl.termUncertain = offered(c, term.season) == Offering.UNKNOWN;
                tp.courses.add(pl);
            }
            // an empty term stays in the plan so a gap (nothing offered that fits) is visible
            plan.terms.add(tp);
            for (Placement pl : tp.courses) {
                placed.add(pl.course.code);
                finished.add(pl.course.code);
            }
        }
        // drop a leading run of empty terms
        while (!plan.terms.isEmpty() && plan.terms.get(0).courses.isEmpty()) plan.terms.remove(0);
        plan.complete = placed.contains(target.code);
        if (!plan.complete) plan.notes.add("I could not fit " + target.code + " in the next " + MAX_TERMS + " terms: something it needs is never "
                + "offered in a term that works.");
        return plan;
    }

    private static boolean cyclic(Map<String, List<String>> deps) {
        for (String start : deps.keySet()) {
            Set<String> seen = new LinkedHashSet<>();
            List<String> stack = new ArrayList<>();
            stack.add(start);
            while (!stack.isEmpty()) {
                String c = stack.remove(stack.size() - 1);
                for (String d : deps.getOrDefault(c, new ArrayList<String>())) {
                    if (d.equals(start)) return true;
                    if (seen.add(d)) stack.add(d);
                }
            }
        }
        return false;
    }
}

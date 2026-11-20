package command.util.art;

import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import command.util.misc.Prerequisites;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pictures for the course lookup: a banner that builds up, and the prerequisite chain drawn as connected boxes. */
public final class CourseArt {


    private CourseArt() { }

    /** Course level as a shade of grey: the deeper the level, the darker. */
    private static Color levelColour(int level) {
        return Ui.mix(Ui.faint(), Ui.text(), (Math.max(1, Math.min(level, 5)) - 1) / 4.0);
    }

    private static String fit(Graphics2D g, String text, Font font, int max) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t) > max) t = t.substring(0, t.length() - 1);
        return t.equals(text) ? t : t.trim() + "…";
    }

    // ------------------------------------------------------------------ banner

    private static final int BANNER_W = 700;
    private static final int BANNER_H = 180;

    private static void chip(Graphics2D g, String text, int x, int y, boolean on, double grow) {
        if (grow <= 0) return;
        g.setFont(Ui.bold(16));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text) + 26;
        int h = 30;
        double scale = Math.min(1, grow);
        int dw = (int) (w * scale);
        int dh = (int) (h * scale);
        int bx = x + (w - dw) / 2;
        int by = y + (h - dh) / 2;
        if (on) Ui.inset(g, bx, by, dw, dh, Ui.select());
        else Ui.control(g, bx, by, dw, dh);
        if (scale >= 1) {
            g.setColor(Ui.text());
            g.drawString(text, x + 13, y + 21);
        }
    }

    private static BufferedImage bannerFrame(BufferedImage bg, Course c, int step) {
        BufferedImage f = GameArt.copy(bg);
        Graphics2D g = f.createGraphics();
        try {
            GameArt.hints(g);
            Color level = levelColour(c.level());
            // a bar in the level's shade slides down the left edge
            g.setColor(level);
            int stripe = (int) (Math.min(1, step / 2.0) * (BANNER_H - Ui.TOP - 24));
            g.fillRect(18, Ui.TOP + 12, 10, stripe);
            // the code slides in from the left
            double slide = 1 - Math.min(1, step / 3.0);
            int codeX = 50 - (int) (slide * 120);
            if (step >= 1) {
                g.setFont(Ui.bold(40));
                g.setColor(Ui.text());
                g.drawString(c.code, codeX, 76);
            }
            // the title fades up
            if (step >= 2) {
                int alpha = (int) (255 * Math.min(1, (step - 1) / 2.0));
                Font font = Ui.bold(22);
                g.setColor(new Color(Ui.text().getRed(), Ui.text().getGreen(), Ui.text().getBlue(), alpha));
                g.setFont(font);
                g.drawString(fit(g, c.title, font, BANNER_W - 100), 52, 108);
            }
            // chips pop in one after another
            List<String> chips = new ArrayList<>();
            if (!c.offered.isEmpty()) chips.add(c.offered);
            if (!c.hours.isEmpty()) chips.add(c.hours);
            if (!c.credits.isEmpty()) chips.add(c.credits + " credits");
            if (c.source.equals("graduate")) chips.add("Graduate");
            int x = 52;
            g.setFont(Ui.bold(16));
            for (int i = 0; i < chips.size(); i++) {
                double grow = (step - 3 - i * 0.6) / 1.0;
                int w = g.getFontMetrics().stringWidth(chips.get(i)) + 26;
                chip(g, chips.get(i), x, 126, i == 0, grow);
                x += w + 10;
            }
        } finally {
            g.dispose();
        }
        return f;
    }

    /** The course code, title and facts building up over a second, then holding. */
    public static byte[] banner(Course c) throws IOException {
        BufferedImage bg = GameArt.canvas(BANNER_W, BANNER_H, "Course");
        List<BufferedImage> frames = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        int steps = 8;
        for (int step = 0; step <= steps; step++) {
            frames.add(bannerFrame(bg, c, step));
            delays.add(step == steps ? 6000 : 110);
        }
        return GifWriter.write(frames, delays, false);
    }

    // -------------------------------------------------------------------- tree

    private static final int MAX_NODES = 14;
    private static final int MAX_DEPTH = 3;
    private static final int BOX_W = 190;
    private static final int BOX_H = 58;
    private static final int COL_GAP = 70;
    private static final int ROW_GAP = 16;

    /** Where a course stands for the person looking: finished, takeable now, or not yet. */
    public enum State { NEUTRAL, DONE, READY, MAYBE, LOCKED }

    /** A course in the tree, how many steps below the target it sits, and where it is drawn. */
    private static final class Node {
        final String code;
        final String title;
        State state = State.NEUTRAL;
        int depth;
        int x;
        int y;
        final List<String> needs = new ArrayList<>();

        Node(String code, String title, int depth) {
            this.code = code;
            this.title = title;
            this.depth = depth;
        }
    }

    /**
     * Collects the course and everything it needs, a few steps deep. A course that is needed in two places is
     * drawn once, at its deepest level.
     */
    static Map<String, Node> gather(CourseCatalog catalog, Course root) {
        Map<String, Node> nodes = new LinkedHashMap<>();
        nodes.put(root.code, new Node(root.code, root.title, 0));
        List<Course> frontier = new ArrayList<>();
        frontier.add(root);
        for (int depth = 1; depth <= MAX_DEPTH && !frontier.isEmpty(); depth++) {
            List<Course> next = new ArrayList<>();
            for (Course c : frontier) {
                for (String code : c.prerequisiteCodes()) {
                    if (nodes.size() >= MAX_NODES && !nodes.containsKey(code)) continue;
                    Course p = catalog.find(code);
                    Node n = nodes.get(code);
                    if (n == null) {
                        n = new Node(code, p == null ? "" : p.title, depth);
                        nodes.put(code, n);
                        if (p != null) next.add(p);
                    }
                    List<String> needs = nodes.get(c.code).needs;
                    if (!needs.contains(code)) needs.add(code);
                }
            }
            frontier = next;
        }
        // A course needed in two places sits at its deepest level, and so does everything it needs, so every arrow
        // runs from a deeper column to a shallower one. Passes are limited so a loop in the catalogue cannot hang this.
        for (Node n : nodes.values()) n.depth = n == nodes.get(root.code) ? 0 : 1;
        for (int pass = 0; pass < nodes.size(); pass++) {
            boolean changed = false;
            for (Node n : nodes.values()) {
                for (String need : n.needs) {
                    Node p = nodes.get(need);
                    if (p != null && p.depth < n.depth + 1 && n.depth + 1 <= nodes.size()) {
                        p.depth = n.depth + 1;
                        changed = true;
                    }
                }
            }
            if (!changed) break;
        }
        return nodes;
    }

    /** Marks every course in the tree against what the person has finished (empty means no personal colouring). */
    private static void markStates(CourseCatalog catalog, Map<String, Node> nodes, Set<String> done) {
        if (done == null || done.isEmpty()) return;
        for (Node n : nodes.values()) {
            Course c = catalog.find(n.code);
            if (done.contains(n.code)) {
                n.state = State.DONE;
            } else if (c == null) {
                n.state = State.LOCKED;
            } else {
                Prerequisites.Status s = Prerequisites.of(c).check(done);
                n.state = s == Prerequisites.Status.MET ? State.READY
                        : s == Prerequisites.Status.UNKNOWN ? State.MAYBE : State.LOCKED;
            }
        }
    }

    /** How far along someone is towards a course. */
    public static final class Progress {
        /** Prerequisite courses in the tree, not counting the target. */
        public final int needed;
        /** How many of them are finished. */
        public final int finished;
        /** Courses that can be taken right now and lead towards the target, the ones closest to it first. */
        public final List<Course> nextUp;
        /** The target itself: {@code READY} means it can be taken now. */
        public final State target;

        Progress(int needed, int finished, List<Course> nextUp, State target) {
            this.needed = needed;
            this.finished = finished;
            this.nextUp = nextUp;
            this.target = target;
        }
    }

    /** @param done the finished courses, with equivalent courses already added (see {@link Prerequisites#closure}) */
    public static Progress progress(CourseCatalog catalog, Course root, Set<String> done) {
        Map<String, Node> nodes = gather(catalog, root);
        markStates(catalog, nodes, done);
        int finished = 0;
        List<Node> ready = new ArrayList<>();
        for (Node n : nodes.values()) {
            if (n.depth == 0) continue;
            if (n.state == State.DONE) finished++;
            else if (n.state == State.READY) ready.add(n);
        }
        // the ones closest to the target come first: taking them moves you furthest along
        ready.sort((a, b) -> a.depth != b.depth ? Integer.compare(a.depth, b.depth) : a.code.compareTo(b.code));
        List<Course> next = new ArrayList<>();
        for (Node n : ready) {
            Course c = catalog.find(n.code);
            if (c != null) next.add(c);
        }
        return new Progress(nodes.size() - 1, finished, next, nodes.get(root.code).state);
    }

    /** @return how many prerequisite courses the tree would show (not counting the target), for deciding to draw one */
    public static int prerequisiteCount(CourseCatalog catalog, Course root) {
        return gather(catalog, root).size() - 1;
    }

    private static void box(Graphics2D g, Node n, boolean target, double grow) {
        if (grow <= 0) return;
        double s = Math.min(1, grow);
        int w = (int) (BOX_W * s);
        int h = (int) (BOX_H * s);
        int x = n.x + (BOX_W - w) / 2;
        int y = n.y + (BOX_H - h) / 2;
        switch (n.state) {
            case DONE: Ui.inset(g, x, y, w, h, Ui.select()); break;
            case LOCKED: Ui.outline(g, x, y, w, h, Ui.control()); break;
            default: Ui.control(g, x, y, w, h); break;
        }
        if (target) Ui.ring(g, x, y, w, h);
        if (s >= 1) {
            if (n.state == State.MAYBE) Ui.dashedRing(g, n.x, n.y, BOX_W, BOX_H);
            Color ink = n.state == State.LOCKED ? Ui.muted() : Ui.text();
            g.setFont(Ui.bold(16));
            g.setColor(ink);
            g.drawString(n.code, n.x + 12, n.y + 24);
            g.setFont(Ui.bold(11));
            if (n.state == State.DONE) {
                g.drawString("done", n.x + BOX_W - 38, n.y + 22);
            } else if (n.state == State.READY) {
                g.drawString("ready", n.x + BOX_W - 40, n.y + 22);
            }
            Font small = Ui.plain(12);
            g.setColor(n.state == State.LOCKED ? Ui.muted() : Ui.text());
            g.setFont(small);
            g.drawString(fit(g, n.title.isEmpty() ? "not in the catalogue" : n.title, small, BOX_W - 20), n.x + 12, n.y + 44);
        }
    }

    private static void arrow(Graphics2D g, Node from, Node to, double progress) {
        if (progress <= 0) return;
        double x1 = from.x + BOX_W;
        double y1 = from.y + BOX_H / 2.0;
        double x2 = to.x;
        double y2 = to.y + BOX_H / 2.0;
        double mx = x1 + (x2 - x1) * Math.min(1, progress);
        double my = y1 + (y2 - y1) * Math.min(1, progress);
        Path2D path = new Path2D.Double();
        path.moveTo(x1, y1);
        path.lineTo(mx, my);
        g.setColor(from.state == State.LOCKED ? Ui.control() : Ui.muted());
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.draw(path);
        if (progress >= 1) {
            Path2D head = new Path2D.Double();
            head.moveTo(x2, y2);
            head.lineTo(x2 - 12, y2 - 7);
            head.lineTo(x2 - 12, y2 + 7);
            head.closePath();
            g.fill(head);
        }
    }

    private static int swatch(Graphics2D g, int x, int y, State state, String label) {
        switch (state) {
            case DONE: Ui.inset(g, x, y, 16, 16, Ui.select()); break;
            case LOCKED: Ui.outline(g, x, y, 16, 16, Ui.control()); break;
            default: Ui.control(g, x, y, 16, 16); break;
        }
        if (state == State.MAYBE) Ui.dashedRing(g, x, y, 16, 16);
        g.setFont(Ui.plain(12));
        g.setColor(Ui.text());
        g.drawString(label, x + 24, y + 12);
        return x + 24 + g.getFontMetrics().stringWidth(label) + 18;
    }

    /** The key, and how far along the person is, under a coloured tree. */
    private static void legend(Graphics2D g, int w, int h, Progress p, Course root) {
        int y = h - 46;
        int x = 40;
        x = swatch(g, x, y, State.DONE, "finished");
        x = swatch(g, x, y, State.READY, "ready to take");
        x = swatch(g, x, y, State.MAYBE, "part unchecked");
        swatch(g, x, y, State.LOCKED, "not yet");
        String line = p.target == State.READY ? "You can take " + root.code + " now."
                : p.target == State.MAYBE ? "You meet everything I can check for " + root.code + "."
                : p.finished + " of " + p.needed + " prerequisite courses finished"
                + (p.nextUp.isEmpty() ? "." : ". Next: " + p.nextUp.get(0).code + ".");
        GameArt.outlined(g, line, Ui.bold(15), w / 2, h - 12, Ui.text());
    }

    /**
     * The course and what it needs, as boxes joined by arrows: the earliest courses on the left, the target on the
     * right. The picture builds from the left, one column at a time.
     *
     * @return the GIF, or null if the course has no prerequisite courses to draw
     */
    public static byte[] prerequisiteTree(CourseCatalog catalog, Course root) throws IOException {
        return prerequisiteTree(catalog, root, null);
    }

    /**
     * The same tree coloured by what the person has finished: green for finished, blue for ready to take now, amber
     * for ready apart from something that cannot be checked, grey for not yet. With no finished courses it is drawn
     * plain.
     *
     * @param done finished courses, with equivalent courses already added; may be null or empty
     */
    public static byte[] prerequisiteTree(CourseCatalog catalog, Course root, Set<String> done) throws IOException {
        Map<String, Node> nodes = gather(catalog, root);
        if (nodes.size() < 2) return null;
        markStates(catalog, nodes, done);
        boolean personal = done != null && !done.isEmpty();
        Progress progress = progress(catalog, root, done);
        int maxDepth = 0;
        for (Node n : nodes.values()) maxDepth = Math.max(maxDepth, n.depth);
        // one column per depth, deepest on the left
        List<List<Node>> columns = new ArrayList<>();
        for (int d = maxDepth; d >= 0; d--) {
            List<Node> col = new ArrayList<>();
            for (Node n : nodes.values()) if (n.depth == d) col.add(n);
            columns.add(col);
        }
        int rows = 1;
        for (List<Node> col : columns) rows = Math.max(rows, col.size());
        int w = 40 + columns.size() * BOX_W + (columns.size() - 1) * COL_GAP + 40;
        int h = 90 + rows * BOX_H + (rows - 1) * ROW_GAP + (personal ? 56 : 30);
        w = Math.max(w, personal ? 600 : 420);
        for (int c = 0; c < columns.size(); c++) {
            List<Node> col = columns.get(c);
            int colHeight = col.size() * BOX_H + (col.size() - 1) * ROW_GAP;
            for (int r = 0; r < col.size(); r++) {
                Node n = col.get(r);
                n.x = 40 + c * (BOX_W + COL_GAP);
                n.y = 70 + (rows * BOX_H + (rows - 1) * ROW_GAP - colHeight) / 2 + r * (BOX_H + ROW_GAP);
            }
        }
        BufferedImage bg = GameArt.canvas(w, h, "Prerequisites - " + root.code);
        List<BufferedImage> frames = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        int steps = columns.size() * 2;
        for (int step = 0; step <= steps; step++) {
            BufferedImage f = GameArt.copy(bg);
            Graphics2D g = f.createGraphics();
            try {
                GameArt.hints(g);
                for (Node n : nodes.values()) {
                    int col = maxDepth - n.depth;
                    double grow = (step - col * 2) / 2.0;
                    for (String need : n.needs) {
                        Node from = nodes.get(need);
                        if (from.depth <= n.depth) continue; // a loop in the catalogue: draw no backwards arrow
                        int fromCol = maxDepth - from.depth;
                        arrow(g, from, n, (step - fromCol * 2 - 1) / 2.0);
                    }
                    box(g, n, n.depth == 0, grow);
                }
                if (step == steps) {
                    if (personal) {
                        legend(g, w, h, progress, root);
                    } else {
                        GameArt.centered(g, "An arrow means \"needed for\". Some prerequisites are alternatives: check the text.",
                                Ui.plain(12), Ui.muted(), w / 2, h - 12);
                    }
                }
            } finally {
                g.dispose();
            }
            frames.add(f);
            delays.add(step == steps ? 8000 : 260);
        }
        return GifWriter.write(frames, delays, false);
    }

    // ----------------------------------------------------------------- ratings

    private static void histogram(Graphics2D g, String title, int[] counts, double average, int x, int y, int w, int h,
                                  Color colour, double grow) {
        g.setFont(Ui.bold(16));
        g.setColor(Ui.text());
        g.drawString(title, x, y - 10);
        int max = 1;
        for (int c : counts) max = Math.max(max, c);
        int barW = (w - 4 * 12) / 5;
        for (int i = 0; i < 5; i++) {
            int bx = x + i * (barW + 12);
            int full = (int) ((h - 42) * counts[i] / (double) max);
            int bh = (int) (full * Math.min(1, grow));
            Ui.inset(g, bx, y, barW, h - 22, Ui.inset());
            if (bh > 1) Ui.control(g, bx + 2, y + h - 22 - bh - 2, barW - 4, bh, colour, colour);
            g.setFont(Ui.bold(13));
            g.setColor(Ui.text());
            String label = Integer.toString(i + 1);
            g.drawString(label, bx + (barW - g.getFontMetrics().stringWidth(label)) / 2, y + h - 4);
            if (counts[i] > 0 && grow >= 1) {
                String n = Integer.toString(counts[i]);
                g.drawString(n, bx + (barW - g.getFontMetrics().stringWidth(n)) / 2, y + h - 22 - bh - 6);
            }
        }
        if (grow >= 1) {
            g.setFont(Ui.bold(20));
            g.setColor(Ui.text());
            g.drawString(String.format("avg %.1f", average), x + w - 80, y - 10);
        }
    }

    /** Two bar charts, how hard and how heavy students found the course, growing from nothing. */
    public static byte[] ratingsChart(Course c, courses.Ratings.Summary s) throws IOException {
        int w = 640;
        int h = 300;
        BufferedImage bg = GameArt.canvas(w, h, "Ratings - " + c.code);
        List<BufferedImage> frames = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        int steps = 6;
        for (int step = 1; step <= steps; step++) {
            BufferedImage f = GameArt.copy(bg);
            Graphics2D g = f.createGraphics();
            try {
                GameArt.hints(g);
                double grow = step / (double) steps;
                GameArt.outlined(g, c.code + ": " + s.count + (s.count == 1 ? " rating" : " ratings"),
                        Ui.bold(18), w / 2, 48, Ui.text());
                histogram(g, "Difficulty", s.difficultyCounts, s.difficulty, 40, 90, 260, 180, Ui.select(), grow);
                histogram(g, "Workload", s.workloadCounts, s.workload, 340, 90, 260, 180, Ui.select(), grow);
            } finally {
                g.dispose();
            }
            frames.add(f);
            delays.add(step == steps ? 6000 : 120);
        }
        return GifWriter.write(frames, delays, false);
    }

    // ----------------------------------------------------------------- roadmap

    /**
     * The plan as columns of boxes, one column per term, earliest on the left, arrows from what is needed to what needs it.
     * The picture builds column by column and ends on the target and the earliest finish.
     */
    public static byte[] roadmap(courses.DegreePlan.Plan plan, int perTerm) throws IOException {
        List<courses.DegreePlan.TermPlan> terms = plan.terms;
        int columns = terms.size();
        if (columns == 0) return null;
        Map<String, Node> nodes = new LinkedHashMap<>();
        int rows = 1;
        for (int c = 0; c < columns; c++) {
            courses.DegreePlan.TermPlan tp = terms.get(c);
            rows = Math.max(rows, tp.courses.size());
            for (courses.DegreePlan.Placement pl : tp.courses) {
                boolean target = pl.course.code.equals(plan.target.code);
                Node n = new Node(pl.course.code, pl.termUncertain ? pl.course.title + " ?" : pl.course.title, target ? 0 : columns - c);
                n.needs.addAll(pl.needs);
                n.state = c == 0 ? State.READY : State.NEUTRAL;
                n.x = 40 + c * (BOX_W + COL_GAP);
                n.y = 92 + pl.course.code.hashCode() * 0; // set below
                nodes.put(pl.course.code, n);
            }
        }
        for (int c = 0; c < columns; c++) {
            courses.DegreePlan.TermPlan tp = terms.get(c);
            int colHeight = tp.courses.size() * BOX_H + Math.max(0, tp.courses.size() - 1) * ROW_GAP;
            int top = 92 + (rows * BOX_H + (rows - 1) * ROW_GAP - colHeight) / 2;
            for (int r = 0; r < tp.courses.size(); r++) {
                Node n = nodes.get(tp.courses.get(r).course.code);
                n.y = top + r * (BOX_H + ROW_GAP);
            }
        }
        int w = Math.max(460, 40 + columns * BOX_W + (columns - 1) * COL_GAP + 40);
        int h = 92 + rows * BOX_H + (rows - 1) * ROW_GAP + 70;
        BufferedImage bg = GameArt.canvas(w, h, "Route - " + plan.target.code);
        List<BufferedImage> frames = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        int steps = columns * 2;
        for (int step = 0; step <= steps; step++) {
            BufferedImage f = GameArt.copy(bg);
            Graphics2D g = f.createGraphics();
            try {
                GameArt.hints(g);
                for (int c = 0; c < columns; c++) {
                    double grow = (step - c * 2) / 2.0;
                    if (grow <= 0) continue;
                    GameArt.centered(g, terms.get(c).term.label(), Ui.bold(15), new Color(Ui.text().getRed(), Ui.text().getGreen(), Ui.text().getBlue(), (int) (255 * Math.min(1, grow))),
                            40 + c * (BOX_W + COL_GAP) + BOX_W / 2, 76);
                    if (terms.get(c).courses.isEmpty()) {
                        GameArt.centered(g, "nothing fits", new Font(Font.DIALOG, Font.ITALIC, 13), Ui.muted(),
                                40 + c * (BOX_W + COL_GAP) + BOX_W / 2, 92 + BOX_H / 2);
                    }
                }
                for (Node n : nodes.values()) {
                    int col = 0;
                    for (int c = 0; c < columns; c++) for (courses.DegreePlan.Placement pl : terms.get(c).courses) if (pl.course.code.equals(n.code)) col = c;
                    for (String need : n.needs) {
                        Node from = nodes.get(need);
                        if (from == null || from.depth <= n.depth) continue;
                        int fromCol = columns - from.depth;
                        arrow(g, from, n, (step - fromCol * 2 - 1) / 2.0);
                    }
                    box(g, n, n.depth == 0, (step - col * 2) / 2.0);
                }
                if (step == steps) {
                    String line = plan.complete ? "Earliest finish: " + plan.finish().label() + " \u00B7 " + columns + (columns == 1 ? " term" : " terms")
                            + " \u00B7 up to " + perTerm + " courses a term" : "No route found in " + columns + " terms";
                    GameArt.outlined(g, line, Ui.bold(15), w / 2, h - 30, plan.complete ? Ui.ok() : Ui.text());
                    GameArt.centered(g, "Bold outline = ready to take now. A ? means the catalogue does not say when it is offered.",
                            Ui.plain(11), Ui.muted(), w / 2, h - 12);
                }
            } finally {
                g.dispose();
            }
            frames.add(f);
            delays.add(step == steps ? 8000 : 260);
        }
        return GifWriter.write(frames, delays, false);
    }
}

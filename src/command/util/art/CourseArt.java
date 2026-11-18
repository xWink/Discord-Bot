package command.util.art;

import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pictures for the course lookup: a banner that builds up, and the prerequisite chain drawn as connected boxes. */
public final class CourseArt {

    private static final Color GOLD = Sticker.GOLD;
    private static final Color INK = Sticker.INK;

    private CourseArt() { }

    private static Color levelColour(int level) {
        switch (level) {
            case 1: return new Color(0x57F287);
            case 2: return new Color(0x3498DB);
            case 3: return new Color(0xE67E22);
            case 4: return new Color(0xED4245);
            default: return new Color(0x9B59B6);
        }
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
    private static final int BANNER_H = 170;

    private static void chip(Graphics2D g, String text, int x, int y, Color fill, double grow) {
        if (grow <= 0) return;
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text) + 26;
        int h = 30;
        double scale = Math.min(1, grow);
        int dw = (int) (w * scale);
        int dh = (int) (h * scale);
        Shape pill = new RoundRectangle2D.Double(x + (w - dw) / 2.0, y + (h - dh) / 2.0, dw, dh, h, h);
        g.setColor(fill);
        g.fill(pill);
        g.setColor(INK);
        g.setStroke(new BasicStroke(3f));
        g.draw(pill);
        if (scale >= 1) {
            g.setColor(Color.WHITE.equals(fill) ? INK : Color.WHITE);
            g.drawString(text, x + 13, y + 21);
        }
    }

    private static BufferedImage bannerFrame(BufferedImage bg, Course c, int step) {
        BufferedImage f = GameArt.copy(bg);
        Graphics2D g = f.createGraphics();
        try {
            GameArt.hints(g);
            Color level = levelColour(c.level());
            // a stripe in the level's colour slides down the left edge
            g.setColor(level);
            int stripe = (int) (Math.min(1, step / 2.0) * (BANNER_H - 36));
            g.fillRoundRect(18, 18, 10, stripe, 8, 8);
            // the code slides in from the left
            double slide = 1 - Math.min(1, step / 3.0);
            int codeX = 50 - (int) (slide * 120);
            if (step >= 1) {
                g.setFont(new Font("SansSerif", Font.BOLD, 40));
                java.awt.font.GlyphVector gv = g.getFont().createGlyphVector(g.getFontRenderContext(), c.code);
                Shape o = gv.getOutline(codeX, 66);
                g.setColor(INK);
                g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(o);
                g.setColor(GOLD);
                g.fill(o);
            }
            // the title fades up
            if (step >= 2) {
                int alpha = (int) (255 * Math.min(1, (step - 1) / 2.0));
                Font font = new Font("SansSerif", Font.BOLD, 22);
                g.setColor(new Color(255, 255, 255, alpha));
                g.setFont(font);
                g.drawString(fit(g, c.title, font, BANNER_W - 100), 52, 100);
            }
            // chips pop in one after another
            List<String> chips = new ArrayList<>();
            if (!c.offered.isEmpty()) chips.add(c.offered);
            if (!c.hours.isEmpty()) chips.add(c.hours);
            if (!c.credits.isEmpty()) chips.add(c.credits + " credits");
            if (c.source.equals("graduate")) chips.add("Graduate");
            int x = 52;
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            for (int i = 0; i < chips.size(); i++) {
                double grow = (step - 3 - i * 0.6) / 1.0;
                int w = g.getFontMetrics().stringWidth(chips.get(i)) + 26;
                chip(g, chips.get(i), x, 120, i == 0 ? level.darker() : new Color(0, 0, 0, 150), grow);
                x += w + 10;
            }
        } finally {
            g.dispose();
        }
        return f;
    }

    /** The course code, title and facts building up over a second, then holding. */
    public static byte[] banner(Course c) throws IOException {
        BufferedImage bg = GameArt.canvas(BANNER_W, BANNER_H);
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

    /** A course in the tree, how many steps below the target it sits, and where it is drawn. */
    private static final class Node {
        final String code;
        final String title;
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

    /** @return how many prerequisite courses the tree would show (not counting the target), for deciding to draw one */
    public static int prerequisiteCount(CourseCatalog catalog, Course root) {
        return gather(catalog, root).size() - 1;
    }

    private static void box(Graphics2D g, Node n, boolean target, double grow) {
        if (grow <= 0) return;
        double s = Math.min(1, grow);
        double w = BOX_W * s;
        double h = BOX_H * s;
        Shape r = new RoundRectangle2D.Double(n.x + (BOX_W - w) / 2, n.y + (BOX_H - h) / 2, w, h, 16, 16);
        g.setColor(target ? new Color(0x4A3F00) : new Color(0, 0, 0, 140));
        g.fill(r);
        g.setColor(target ? GOLD : INK);
        g.setStroke(new BasicStroke(target ? 4f : 3f));
        g.draw(r);
        if (s >= 1) {
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(target ? GOLD : Color.WHITE);
            g.drawString(n.code, n.x + 12, n.y + 24);
            Font small = new Font("SansSerif", Font.PLAIN, 12);
            g.setColor(new Color(255, 255, 255, 190));
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
        g.setColor(new Color(255, 221, 0, 200));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
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

    /**
     * The course and what it needs, as boxes joined by arrows: the earliest courses on the left, the target on the
     * right. The picture builds from the left, one column at a time.
     *
     * @return the GIF, or null if the course has no prerequisite courses to draw
     */
    public static byte[] prerequisiteTree(CourseCatalog catalog, Course root) throws IOException {
        Map<String, Node> nodes = gather(catalog, root);
        if (nodes.size() < 2) return null;
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
        int h = 90 + rows * BOX_H + (rows - 1) * ROW_GAP + 30;
        w = Math.max(w, 420);
        for (int c = 0; c < columns.size(); c++) {
            List<Node> col = columns.get(c);
            int colHeight = col.size() * BOX_H + (col.size() - 1) * ROW_GAP;
            for (int r = 0; r < col.size(); r++) {
                Node n = col.get(r);
                n.x = 40 + c * (BOX_W + COL_GAP);
                n.y = 70 + (rows * BOX_H + (rows - 1) * ROW_GAP - colHeight) / 2 + r * (BOX_H + ROW_GAP);
            }
        }
        BufferedImage bg = GameArt.canvas(w, h);
        List<BufferedImage> frames = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        int steps = columns.size() * 2;
        for (int step = 0; step <= steps; step++) {
            BufferedImage f = GameArt.copy(bg);
            Graphics2D g = f.createGraphics();
            try {
                GameArt.hints(g);
                GameArt.outlined(g, "Prerequisites for " + root.code, new Font("SansSerif", Font.BOLD, 24), w / 2, 44, GOLD);
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
                    GameArt.centered(g, "An arrow means \"needed for\". Some prerequisites are alternatives: check the text.",
                            new Font("SansSerif", Font.PLAIN, 12), new Color(255, 255, 255, 160), w / 2, h - 12);
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
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(GOLD);
        g.drawString(title, x, y - 10);
        int max = 1;
        for (int c : counts) max = Math.max(max, c);
        int barW = (w - 4 * 12) / 5;
        for (int i = 0; i < 5; i++) {
            int bx = x + i * (barW + 12);
            int full = (int) ((h - 30) * counts[i] / (double) max);
            int bh = (int) (full * Math.min(1, grow));
            g.setColor(new Color(0, 0, 0, 120));
            g.fill(new RoundRectangle2D.Double(bx, y, barW, h - 22, 8, 8));
            g.setColor(colour);
            g.fill(new RoundRectangle2D.Double(bx, y + h - 22 - bh, barW, Math.max(bh, 0), 8, 8));
            g.setColor(INK);
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Double(bx, y, barW, h - 22, 8, 8));
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(Color.WHITE);
            String label = Integer.toString(i + 1);
            g.drawString(label, bx + (barW - g.getFontMetrics().stringWidth(label)) / 2, y + h - 4);
            if (counts[i] > 0 && grow >= 1) {
                String n = Integer.toString(counts[i]);
                g.drawString(n, bx + (barW - g.getFontMetrics().stringWidth(n)) / 2, y + h - 22 - bh - 4);
            }
        }
        if (grow >= 1) {
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.setColor(Color.WHITE);
            g.drawString(String.format("avg %.1f", average), x + w - 80, y - 10);
        }
    }

    /** Two bar charts, how hard and how heavy students found the course, growing from nothing. */
    public static byte[] ratingsChart(Course c, courses.Ratings.Summary s) throws IOException {
        int w = 640;
        int h = 290;
        BufferedImage bg = GameArt.canvas(w, h);
        List<BufferedImage> frames = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        int steps = 6;
        for (int step = 1; step <= steps; step++) {
            BufferedImage f = GameArt.copy(bg);
            Graphics2D g = f.createGraphics();
            try {
                GameArt.hints(g);
                double grow = step / (double) steps;
                GameArt.outlined(g, c.code + " \u00B7 " + s.count + (s.count == 1 ? " rating" : " ratings"),
                        new Font("SansSerif", Font.BOLD, 22), w / 2, 36, GOLD);
                histogram(g, "Difficulty", s.difficultyCounts, s.difficulty, 40, 80, 260, 180, new Color(0xE67E22), grow);
                histogram(g, "Workload", s.workloadCounts, s.workload, 340, 80, 260, 180, new Color(0x3498DB), grow);
            } finally {
                g.dispose();
            }
            frames.add(f);
            delays.add(step == steps ? 6000 : 120);
        }
        return GifWriter.write(frames, delays, false);
    }
}

package command.util.art;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Random;

/**
 * Dresses the server mascot. The base art is {@code art/mascot.png}; every prop is drawn on top in the same
 * style: flat red and gold fills with a thick black outline. Cosmetic codes pick the props, one per slot
 * (head, eyes, side, chest, background), so any loadout looks coherent.
 */
public final class Sticker {

    /** The server colours. */
    public static final Color RED = new Color(0xFF0000);
    public static final Color DARK_RED = new Color(0xB30000);
    public static final Color GOLD = new Color(0xFFDD00);
    public static final Color DEEP_GOLD = new Color(0xE0A800);
    public static final Color INK = new Color(0x111111);
    public static final Color WHITE = new Color(0xFFFFFF);

    // The virtual canvas, in mascot pixels. The mascot sits at (OX, OY); props use mascot coordinates.
    private static final int OX = 330;
    private static final int OY = 520;
    private static final int W = 1900;
    private static final int H = 1800;
    private static final float LINE = 13f;

    private static BufferedImage mascot;

    private Sticker() { }

    private static synchronized BufferedImage mascot() throws IOException {
        if (mascot == null) {
            try (InputStream in = Sticker.class.getClassLoader().getResourceAsStream("art/mascot.png")) {
                if (in == null) throw new IOException("Missing art/mascot.png");
                mascot = ImageIO.read(in);
            }
        }
        return mascot;
    }

    /**
     * @param codes the cosmetic codes being worn (may be empty for the plain mascot)
     * @param size  width and height of the square result in pixels
     * @param seed  makes confetti and sparkles land the same way every time for a player
     */
    public static BufferedImage render(Collection<String> codes, int size, long seed) throws IOException {
        BufferedImage canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.translate(OX, OY);
            compose(g, codes, seed);
        } finally {
            g.dispose();
        }
        return fit(crop(canvas), size);
    }

    /** Trims transparent margins and pads to a square so the art fills the frame. */
    private static BufferedImage crop(BufferedImage src) {
        int minX = src.getWidth(), minY = src.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                if ((src.getRGB(x, y) >>> 24) > 8) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) return src;
        int side = Math.max(maxX - minX, maxY - minY) + 2;
        int pad = (int) (side * 0.04);
        int total = side + pad * 2;
        BufferedImage out = new BufferedImage(total, total, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, pad + (side - (maxX - minX)) / 2 - minX, pad + (side - (maxY - minY)) / 2 - minY, null);
        g.dispose();
        return out;
    }

    private static boolean has(Collection<String> codes, String prefix) {
        for (String c : codes) if (c.startsWith(prefix)) return true;
        return false;
    }

    private static void compose(Graphics2D g, Collection<String> codes, long seed) throws IOException {
        Random r = new Random(seed);

        // behind the mascot
        if (has(codes, "fx-fireworks")) fireworks(g, r);
        if (has(codes, "season-")) laurel(g);
        if (has(codes, "flair-day")) sparkles(g, r);
        if (has(codes, "banner-club")) flag(g, -170, 330);
        if (has(codes, "fx-trophy")) trophy(g, 1330, 680, 1.0);

        g.drawImage(mascot(), 0, 0, null);

        // on the mascot: one prop per slot
        if (has(codes, "fx-stars")) starEyes(g);
        if (has(codes, "bundle-weekend")) crown(g);
        else if (has(codes, "victory-fx")) partyHat(g);
        else if (has(codes, "title-week") || has(codes, "alumni")) gradCap(g);
        if (has(codes, "alumni")) diploma(g, 190, 1010);
        if (has(codes, "badge-month")) medal(g, 400, 1010, GOLD);
        else if (has(codes, "badge-set")) medal(g, 400, 1010, new Color(0xD9DEE6));
        if (has(codes, "gauntlet-")) puzzle(g, -200, 90, 230);

        // in front: confetti and star sparkle
        if (has(codes, "victory-fx")) confetti(g, r);
        if (has(codes, "fx-stars")) starfall(g, r);
    }

    // ----------------------------------------------------------- primitives

    private static void outlined(Graphics2D g, Shape s, Color fill) {
        g.setColor(fill);
        g.fill(s);
        g.setColor(INK);
        g.setStroke(new BasicStroke(LINE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    private static Path2D poly(double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
        p.closePath();
        return p;
    }

    private static Path2D star(double cx, double cy, double outer, double inner, int points, double rotation) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < points * 2; i++) {
            double a = rotation - Math.PI / 2 + i * Math.PI / points;
            double rad = i % 2 == 0 ? outer : inner;
            double x = cx + Math.cos(a) * rad;
            double y = cy + Math.sin(a) * rad;
            if (i == 0) p.moveTo(x, y);
            else p.lineTo(x, y);
        }
        p.closePath();
        return p;
    }

    private static void line(Graphics2D g, double x1, double y1, double x2, double y2, Color c, float w) {
        g.setStroke(new BasicStroke(w + LINE * 0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(INK);
        g.draw(new java.awt.geom.Line2D.Double(x1, y1, x2, y2));
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(c);
        g.draw(new java.awt.geom.Line2D.Double(x1, y1, x2, y2));
    }

    private static void sparkle(Graphics2D g, double cx, double cy, double r, Color fill) {
        Path2D p = new Path2D.Double();
        double in = r * 0.28;
        p.moveTo(cx, cy - r);
        p.quadTo(cx + in, cy - in, cx + r, cy);
        p.quadTo(cx + in, cy + in, cx, cy + r);
        p.quadTo(cx - in, cy + in, cx - r, cy);
        p.quadTo(cx - in, cy - in, cx, cy - r);
        p.closePath();
        outlined(g, p, fill);
    }

    private static BufferedImage fit(BufferedImage src, int size) {
        double scale = Math.min((double) size / src.getWidth(), (double) size / src.getHeight());
        int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(src.getHeight() * scale));
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        // step down in halves for a cleaner result than one big jump
        BufferedImage cur = src;
        int cw = src.getWidth();
        int ch = src.getHeight();
        while (cw / 2 >= w * 1.2) {
            cw /= 2;
            ch /= 2;
            BufferedImage step = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
            Graphics2D sg = step.createGraphics();
            sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            sg.drawImage(cur, 0, 0, cw, ch, null);
            sg.dispose();
            cur = step;
        }
        g.drawImage(cur, (size - w) / 2, (size - h) / 2, w, h, null);
        g.dispose();
        return out;
    }

    // ---------------------------------------------------------------- props

    private static void partyHat(Graphics2D g) {
        AffineTransform at = g.getTransform();
        g.translate(830, 215);
        g.scale(1.25, 1.25);
        g.translate(-830, -215);
        g.rotate(Math.toRadians(-9), 830, 190);
        Path2D cone = poly(690, 215, 975, 215, 835, -210);
        outlined(g, cone, GOLD);
        // stripes clipped to the cone
        Shape old = g.getClip();
        g.setClip(cone);
        g.setColor(RED);
        g.fill(poly(640, 150, 1000, 150, 1000, 60, 640, 60));
        g.fill(poly(640, -10, 1000, -10, 1000, -100, 640, -100));
        g.setClip(old);
        g.setColor(INK);
        g.setStroke(new BasicStroke(LINE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(cone);
        outlined(g, new RoundRectangle2D.Double(670, 195, 325, 50, 40, 40), RED);
        outlined(g, new Ellipse2D.Double(795, -255, 85, 85), GOLD);
        g.setTransform(at);
    }

    private static void crown(Graphics2D g) {
        AffineTransform at = g.getTransform();
        g.translate(830, 225);
        g.scale(1.3, 1.3);
        g.translate(-830, -225);
        g.rotate(Math.toRadians(-8), 830, 190);
        Path2D c = new Path2D.Double();
        c.moveTo(690, 225);
        c.lineTo(670, 20);
        c.lineTo(760, 110);
        c.lineTo(835, -60);
        c.lineTo(910, 110);
        c.lineTo(1000, 20);
        c.lineTo(975, 225);
        c.closePath();
        outlined(g, c, GOLD);
        outlined(g, new RoundRectangle2D.Double(680, 190, 305, 55, 28, 28), DEEP_GOLD);
        outlined(g, new Ellipse2D.Double(800, 120, 62, 62), RED);
        outlined(g, new Ellipse2D.Double(700, 132, 42, 42), RED);
        outlined(g, new Ellipse2D.Double(920, 132, 42, 42), RED);
        outlined(g, new Ellipse2D.Double(655, -5, 36, 36), WHITE);
        outlined(g, new Ellipse2D.Double(818, -105, 36, 36), WHITE);
        outlined(g, new Ellipse2D.Double(985, -5, 36, 36), WHITE);
        g.setTransform(at);
    }

    private static void gradCap(Graphics2D g) {
        AffineTransform at = g.getTransform();
        g.rotate(Math.toRadians(-8), 830, 190);
        outlined(g, poly(745, 95, 745, 215, 835, 255, 925, 215, 925, 95), new Color(0x2A2A2A));
        outlined(g, poly(640, 75, 840, -20, 1040, 75, 840, 170), new Color(0x1A1A1A));
        outlined(g, new Ellipse2D.Double(818, 52, 44, 44), GOLD);
        line(g, 840, 74, 1010, 95, GOLD, 10);
        line(g, 1010, 95, 1020, 215, GOLD, 10);
        outlined(g, new RoundRectangle2D.Double(998, 205, 46, 70, 14, 14), GOLD);
        g.setTransform(at);
    }

    /** A rolled diploma tied with a red ribbon. */
    private static void diploma(Graphics2D g, double cx, double cy) {
        AffineTransform at = g.getTransform();
        g.translate(cx, cy);
        g.scale(1.3, 1.3);
        g.translate(-cx, -cy);
        g.rotate(Math.toRadians(-14), cx, cy);
        Color paper = new Color(0xFFF4C9);
        outlined(g, new RoundRectangle2D.Double(cx - 150, cy - 48, 300, 96, 40, 40), paper);
        outlined(g, new Ellipse2D.Double(cx - 178, cy - 52, 56, 104), new Color(0xF2DC9B));
        outlined(g, new Ellipse2D.Double(cx + 122, cy - 52, 56, 104), new Color(0xF2DC9B));
        outlined(g, new RoundRectangle2D.Double(cx - 22, cy - 56, 44, 112, 14, 14), RED);
        outlined(g, poly(cx - 4, cy + 50, cx - 40, cy + 130, cx + 4, cy + 100), RED);
        outlined(g, poly(cx + 4, cy + 50, cx + 40, cy + 130, cx - 4, cy + 100), DARK_RED);
        g.setTransform(at);
    }

    private static void starEyes(Graphics2D g) {
        outlined(g, star(738, 372, 74, 34, 5, 0.1), GOLD);
        outlined(g, star(1035, 328, 66, 30, 5, -0.1), GOLD);
        g.setColor(WHITE);
        g.fill(new Ellipse2D.Double(722, 340, 20, 20));
        g.fill(new Ellipse2D.Double(1022, 300, 18, 18));
    }

    private static void starfall(Graphics2D g, Random r) {
        double[][] spots = {{-60, 60, 60}, {1230, 40, 70}, {1290, 460, 46}, {-100, 560, 52}, {560, -40, 40}, {1060, 820, 44}};
        for (double[] s : spots) sparkle(g, s[0] + r.nextInt(30), s[1] + r.nextInt(30), s[2], GOLD);
    }

    private static void medal(Graphics2D g, double cx, double cy, Color metal) {
        Path2D left = poly(cx - 110, cy - 190, cx - 30, cy - 190, cx + 50, cy - 40, cx - 20, cy - 20);
        Path2D right = poly(cx + 110, cy - 190, cx + 30, cy - 190, cx - 50, cy - 40, cx + 20, cy - 20);
        outlined(g, left, RED);
        outlined(g, right, GOLD);
        outlined(g, new Ellipse2D.Double(cx - 100, cy - 60, 200, 200), metal);
        g.setColor(INK);
        g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Ellipse2D.Double(cx - 70, cy - 30, 140, 140));
        outlined(g, star(cx, cy + 40, 52, 22, 5, 0), RED);
    }

    private static void trophy(Graphics2D g, double cx, double cy, double s) {
        AffineTransform at = g.getTransform();
        g.translate(cx, cy);
        g.scale(s, s);
        // handles
        g.setStroke(new BasicStroke(40f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(INK);
        g.draw(new java.awt.geom.Arc2D.Double(-190, -170, 120, 150, 90, 200, java.awt.geom.Arc2D.OPEN));
        g.draw(new java.awt.geom.Arc2D.Double(70, -170, 120, 150, 250, 200, java.awt.geom.Arc2D.OPEN));
        g.setStroke(new BasicStroke(22f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(GOLD);
        g.draw(new java.awt.geom.Arc2D.Double(-190, -170, 120, 150, 90, 200, java.awt.geom.Arc2D.OPEN));
        g.draw(new java.awt.geom.Arc2D.Double(70, -170, 120, 150, 250, 200, java.awt.geom.Arc2D.OPEN));
        Path2D cup = new Path2D.Double();
        cup.moveTo(-130, -230);
        cup.lineTo(130, -230);
        cup.curveTo(130, -20, 70, 40, 0, 40);
        cup.curveTo(-70, 40, -130, -20, -130, -230);
        cup.closePath();
        outlined(g, cup, GOLD);
        outlined(g, star(0, -110, 62, 27, 5, 0), RED);
        outlined(g, new RoundRectangle2D.Double(-24, 30, 48, 120, 14, 14), DEEP_GOLD);
        outlined(g, new RoundRectangle2D.Double(-110, 140, 220, 54, 22, 22), GOLD);
        outlined(g, new RoundRectangle2D.Double(-140, 190, 280, 60, 22, 22), RED);
        g.setTransform(at);
    }

    private static void flag(Graphics2D g, double x, double y) {
        line(g, x, y, x, y + 760, new Color(0xE8E8E8), 18);
        outlined(g, new Ellipse2D.Double(x - 26, y - 44, 52, 52), GOLD);
        Path2D f = new Path2D.Double();
        f.moveTo(x + 10, y + 10);
        f.curveTo(x + 120, y - 40, x + 220, y + 70, x + 340, y + 20);
        f.lineTo(x + 340, y + 250);
        f.curveTo(x + 220, y + 300, x + 120, y + 190, x + 10, y + 240);
        f.closePath();
        outlined(g, f, RED);
        Shape old = g.getClip();
        g.setClip(f);
        g.setColor(GOLD);
        g.fill(poly(x + 150, y - 60, x + 230, y - 60, x + 190, y + 330, x + 110, y + 330));
        g.setClip(old);
        g.setColor(INK);
        g.setStroke(new BasicStroke(LINE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(f);
    }

    private static void puzzle(Graphics2D g, double x, double y, double s) {
        Path2D p = new Path2D.Double();
        p.append(new RoundRectangle2D.Double(x, y, s, s, 30, 30), false);
        java.awt.geom.Area a = new java.awt.geom.Area(p);
        a.add(new java.awt.geom.Area(new Ellipse2D.Double(x + s * 0.32, y - s * 0.2, s * 0.36, s * 0.36)));
        a.add(new java.awt.geom.Area(new Ellipse2D.Double(x + s * 0.84, y + s * 0.32, s * 0.36, s * 0.36)));
        a.subtract(new java.awt.geom.Area(new Ellipse2D.Double(x - s * 0.18, y + s * 0.32, s * 0.36, s * 0.36)));
        outlined(g, a, RED);
        sparkle(g, x + s * 0.5, y + s * 0.55, s * 0.2, GOLD);
    }

    /** Two laurel branches that rise from the bottom centre and frame the mascot. */
    private static void laurel(Graphics2D g) {
        for (int side = -1; side <= 1; side += 2) {
            double px = 0, py = 0;
            java.util.List<double[]> pts = new java.util.ArrayList<>();
            for (int i = 0; i <= 24; i++) {
                double t = i / 24.0;
                double x = 609 + side * 690 * Math.sin(t * Math.PI / 2);
                double y = 1060 - 700 * (1 - Math.cos(t * Math.PI / 2));
                pts.add(new double[]{x, y});
            }
            for (int i = 0; i < pts.size() - 1; i++) {
                line(g, pts.get(i)[0], pts.get(i)[1], pts.get(i + 1)[0], pts.get(i + 1)[1], DEEP_GOLD, 12);
            }
            for (int i = 2; i < pts.size() - 1; i += 2) {
                double[] p0 = pts.get(i);
                double[] p1 = pts.get(i + 1);
                double dir = Math.atan2(p1[1] - p0[1], p1[0] - p0[0]);
                for (int leaf = -1; leaf <= 1; leaf += 2) {
                    AffineTransform at = g.getTransform();
                    g.rotate(dir + leaf * 0.85 + Math.PI / 2, p0[0], p0[1]);
                    // the leaf grows from the stem, pointing along the branch
                    g.rotate(-leaf * 0.85 * 0.0, p0[0], p0[1]);
                    outlined(g, new Ellipse2D.Double(p0[0] - 30, p0[1] - 150, 60, 150), (i / 2 + (leaf > 0 ? 1 : 0)) % 2 == 0 ? GOLD : DEEP_GOLD);
                    g.setTransform(at);
                }
            }
        }
        outlined(g, poly(609, 1090, 470, 1000, 480, 1180), RED);
        outlined(g, poly(609, 1090, 748, 1000, 738, 1180), RED);
        outlined(g, new Ellipse2D.Double(574, 1055, 70, 70), GOLD);
    }

    private static void sparkles(Graphics2D g, Random r) {
        double[][] spots = {{480, -60, 60}, {1180, -20, 46}, {-80, 330, 52}, {1290, 560, 62}, {-30, 880, 44}, {1000, 940, 40}};
        for (double[] s : spots) sparkle(g, s[0] + r.nextInt(24), s[1] + r.nextInt(24), s[2], GOLD);
    }

    private static void confetti(Graphics2D g, Random r) {
        Color[] cs = {RED, GOLD, WHITE, DARK_RED};
        int placed = 0;
        int guard = 0;
        while (placed < 26 && guard++ < 400) {
            double x = -280 + r.nextInt(W - 100);
            double y = -430 + r.nextInt(H - 200);
            // keep the face and the hat clear
            if (x > 200 && x < 1180 && y > -300 && y < 980) continue;
            AffineTransform at = g.getTransform();
            g.rotate(r.nextDouble() * Math.PI, x, y);
            Color c = cs[r.nextInt(cs.length)];
            int kind = r.nextInt(3);
            g.translate(x, y);
            g.scale(1.7, 1.7);
            g.translate(-x, -y);
            if (kind == 0) outlined(g, new RoundRectangle2D.Double(x, y, 46 + r.nextInt(30), 26, 8, 8), c);
            else if (kind == 1) outlined(g, new Ellipse2D.Double(x, y, 32, 32), c);
            else outlined(g, poly(x, y, x + 44, y, x + 22, y + 40), c);
            g.setTransform(at);
            placed++;
        }
    }

    private static void fireworks(Graphics2D g, Random r) {
        double[][] bursts = {{-60, 90, 240}, {1290, 40, 270}, {1360, 800, 230}, {-150, 800, 210}, {640, -290, 200}};
        Color[] cs = {GOLD, RED, GOLD, RED, GOLD};
        for (int b = 0; b < bursts.length; b++) {
            int rays = 12;
            for (int k = 0; k < rays; k++) {
                double a = k * 2 * Math.PI / rays + b;
                double len = bursts[b][2];
                line(g, bursts[b][0] + Math.cos(a) * len * 0.35, bursts[b][1] + Math.sin(a) * len * 0.35,
                        bursts[b][0] + Math.cos(a) * len, bursts[b][1] + Math.sin(a) * len, cs[(b + k) % cs.length], 24);
            }
            sparkle(g, bursts[b][0], bursts[b][1], 62, WHITE);
        }
    }
}

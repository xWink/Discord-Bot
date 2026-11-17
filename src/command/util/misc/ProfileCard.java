package command.util.misc;

import economy.Cosmetics;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Draws the profile card image. What a player wears changes how it looks:
 * the weekend bundle recolours the card and doubles its frame, flair adds a
 * gold glow, titles and team banners get ribbons, badges become icons, and a
 * victory effect scatters its own particles. Everything is drawn with shapes,
 * so it renders the same on any server (no emoji fonts needed).
 */
public final class ProfileCard {

    public static final int WIDTH = 900;
    public static final int HEIGHT = 440;

    private ProfileCard() { }

    /** Numbers shown on the card. */
    public static final class Stats {
        public String name = "Player";
        public int bangs;
        public int deaths;
        public int jams;
        public double survival = 100;
        public int streak;
        public int karma;
        public int achievements;
        public int achievementsTotal;
        public BufferedImage avatar; // may be null
    }

    public static byte[] render(Stats s, Cosmetics.Loadout loadout, long seed) throws IOException {
        System.setProperty("java.awt.headless", "true");
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            draw(g, s, loadout, seed);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    // ---------------------------------------------------------------- theme

    private static final class Theme {
        Color top = new Color(0x2B2D31);
        Color bottom = new Color(0x1E1F22);
        Color accent = new Color(0x5865F2);
        boolean doubleFrame;
        boolean glow;
    }

    private static Theme themeFor(Cosmetics.Loadout l) {
        Theme t = new Theme();
        if (l.has("flair-day")) {
            t.accent = new Color(0xF5C542);
            t.glow = true;
        }
        if (l.has("bundle-weekend")) {
            t.top = new Color(0x7B2FF7);
            t.bottom = new Color(0xE0457B);
            t.accent = new Color(0xFFD166);
            t.doubleFrame = true;
        }
        if (l.has("banner-club") && !l.has("bundle-weekend")) {
            t.top = new Color(0x0F4C5C);
            t.bottom = new Color(0x1FA3A3);
            t.accent = new Color(0x9BF6E3);
        }
        return t;
    }

    // ----------------------------------------------------------------- draw

    private static void draw(Graphics2D g, Stats s, Cosmetics.Loadout l, long seed) {
        Theme t = themeFor(l);
        Shape card = new RoundRectangle2D.Float(8, 8, WIDTH - 16, HEIGHT - 16, 36, 36);

        g.setPaint(new GradientPaint(0, 0, t.top, WIDTH, HEIGHT, t.bottom));
        g.fill(card);

        // soft inner highlight
        g.setClip(card);
        g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 40), 0, HEIGHT / 2f, new Color(255, 255, 255, 0)));
        g.fillRect(0, 0, WIDTH, HEIGHT / 2);

        drawEffectParticles(g, l, seed);
        g.setClip(null);

        // frame
        if (t.glow) {
            for (int i = 10; i >= 1; i--) {
                g.setColor(new Color(t.accent.getRed(), t.accent.getGreen(), t.accent.getBlue(), 14));
                g.setStroke(new BasicStroke(i * 2.2f));
                g.draw(card);
            }
        }
        g.setColor(t.accent);
        g.setStroke(new BasicStroke(5f));
        g.draw(card);
        if (t.doubleFrame) {
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Float(22, 22, WIDTH - 44, HEIGHT - 44, 26, 26));
        }

        drawAvatar(g, s, t, 44, 44, 150);
        drawNameBlock(g, s, l, t);
        drawStats(g, s, 44, 232);
        drawBadges(g, l, t, 44, HEIGHT - 86);

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(255, 255, 255, 110));
        g.drawString("GryphCoin profile", WIDTH - 150, HEIGHT - 26);
    }

    private static void drawAvatar(Graphics2D g, Stats s, Theme t, int x, int y, int size) {
        Shape circle = new Ellipse2D.Float(x, y, size, size);
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Float(x + 4, y + 6, size, size));
        java.awt.Shape old = g.getClip();
        g.setClip(circle);
        if (s.avatar != null) {
            g.drawImage(s.avatar, x, y, size, size, null);
        } else {
            g.setPaint(new GradientPaint(x, y, new Color(0x4F545C), x + size, y + size, new Color(0x2B2D31)));
            g.fill(circle);
            g.setFont(new Font("SansSerif", Font.BOLD, 72));
            g.setColor(Color.WHITE);
            String initial = s.name.isEmpty() ? "?" : s.name.substring(0, 1).toUpperCase();
            FontMetrics fm = g.getFontMetrics();
            g.drawString(initial, x + (size - fm.stringWidth(initial)) / 2, y + (size + fm.getAscent() - fm.getDescent()) / 2);
        }
        g.setClip(old);
        g.setColor(t.accent);
        g.setStroke(new BasicStroke(5f));
        g.draw(circle);
    }

    private static void drawNameBlock(Graphics2D g, Stats s, Cosmetics.Loadout l, Theme t) {
        int x = 224;
        g.setFont(new Font("SansSerif", Font.BOLD, 44));
        g.setColor(Color.WHITE);
        g.drawString(fit(g, s.name, WIDTH - x - 60), x, 96);

        int y = 124;
        if (l.has("title-week") || l.has("bundle-weekend")) {
            String title = l.has("bundle-weekend") ? "the Weekender" : "the Scholar";
            ribbon(g, x, y, title, t.accent, new Color(0, 0, 0, 120), new Font("SansSerif", Font.BOLD | Font.ITALIC, 22));
            y += 44;
        }
        if (l.has("banner-club")) {
            String team = l.team != null ? l.team : "Team";
            flagRibbon(g, x, y, team, t.accent);
        }
    }

    private static void ribbon(Graphics2D g, int x, int y, String text, Color text1, Color fill, Font font) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text) + 40;
        Path2D p = new Path2D.Float();
        p.moveTo(x, y);
        p.lineTo(x + w + 14, y);
        p.lineTo(x + w, y + 17);
        p.lineTo(x + w + 14, y + 34);
        p.lineTo(x, y + 34);
        p.closePath();
        g.setColor(fill);
        g.fill(p);
        g.setColor(text1);
        g.setStroke(new BasicStroke(2f));
        g.draw(p);
        g.drawString(text, x + 18, y + 25);
    }

    private static void flagRibbon(Graphics2D g, int x, int y, String team, Color accent) {
        drawFlag(g, x + 4, y + 2, 30, accent);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        team = fit(g, team, 400);
        g.setColor(new Color(255, 255, 255, 235));
        g.drawString(team + " banner", x + 44, y + 25);
    }

    private static void drawStats(Graphics2D g, Stats s, int x, int y) {
        String[][] cells = {
                {"Bangs", Integer.toString(s.bangs)},
                {"Deaths", Integer.toString(s.deaths)},
                {"Jams", Integer.toString(s.jams)},
                {"Survival", String.format("%.1f%%", s.survival)},
                {"Streak", Integer.toString(s.streak)},
                {"Karma", Integer.toString(s.karma)},
                {"Unlocks", s.achievements + "/" + s.achievementsTotal},
        };
        int cellW = 108;
        int gap = 8;
        for (int i = 0; i < cells.length; i++) {
            int cx = x + i * (cellW + gap);
            g.setColor(new Color(0, 0, 0, 95));
            g.fill(new RoundRectangle2D.Float(cx, y, cellW, 74, 16, 16));
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(new Color(255, 255, 255, 170));
            g.drawString(cells[i][0], cx + 14, y + 26);
            g.setFont(new Font("SansSerif", Font.BOLD, 28));
            g.setColor(Color.WHITE);
            g.drawString(cells[i][1], cx + 14, y + 58);
        }
    }

    private static void drawBadges(Graphics2D g, Cosmetics.Loadout l, Theme t, int x, int y) {
        int step = 56;
        int i = 0;
        for (String code : l.codes) {
            int cx = x + 22 + i * step;
            int cy = y + 22;
            boolean drawn = true;
            if (code.equals("badge-set")) medal(g, cx, cy, 20, new Color(0xC0C7D1));
            else if (code.equals("badge-month")) medal(g, cx, cy, 20, new Color(0xFFB703));
            else if (code.startsWith("season-")) trophy(g, cx, cy, 22);
            else if (code.startsWith("gauntlet-")) puzzle(g, cx, cy, 20);
            else if (code.equals("flair-day")) star(g, cx, cy, 22, 9, 4, t.accent);
            else if (code.equals("bundle-weekend")) star(g, cx, cy, 22, 10, 5, new Color(0xFFD166));
            else drawn = false;
            if (drawn) i++;
        }
        if (i == 0) {
            g.setFont(new Font("SansSerif", Font.ITALIC, 15));
            g.setColor(new Color(255, 255, 255, 120));
            g.drawString("No badges yet. See !shop and !equip.", x, y + 28);
        }
    }

    // ------------------------------------------------------------- particles

    private static void drawEffectParticles(Graphics2D g, Cosmetics.Loadout l, long seed) {
        Random r = new Random(seed);
        if (l.has("victory-fx")) {
            Color[] cs = {new Color(0xFF595E), new Color(0xFFCA3A), new Color(0x8AC926), new Color(0x1982C4), new Color(0x6A4C93)};
            for (int i = 0; i < 60; i++) {
                int px = r.nextInt(WIDTH), py = r.nextInt(HEIGHT);
                g.setColor(withAlpha(cs[r.nextInt(cs.length)], 170));
                java.awt.geom.AffineTransform at = g.getTransform();
                g.rotate(r.nextDouble() * Math.PI, px, py);
                g.fillRect(px, py, 6 + r.nextInt(8), 3 + r.nextInt(4));
                g.setTransform(at);
            }
        }
        if (l.has("fx-fireworks")) {
            Color[] cs = {new Color(0xFF9F1C), new Color(0xFF3D81), new Color(0x2EC4B6), new Color(0xFFFFFF)};
            for (int b = 0; b < 4; b++) {
                int cx = 480 + r.nextInt(380), cy = 40 + r.nextInt(220);
                Color c = cs[r.nextInt(cs.length)];
                int rays = 14;
                for (int k = 0; k < rays; k++) {
                    double a = k * 2 * Math.PI / rays;
                    int len = 26 + r.nextInt(22);
                    g.setColor(withAlpha(c, 200));
                    g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine((int) (cx + Math.cos(a) * 8), (int) (cy + Math.sin(a) * 8),
                            (int) (cx + Math.cos(a) * len), (int) (cy + Math.sin(a) * len));
                    g.fill(new Ellipse2D.Float((float) (cx + Math.cos(a) * (len + 4)) - 2, (float) (cy + Math.sin(a) * (len + 4)) - 2, 4, 4));
                }
            }
        }
        if (l.has("fx-trophy")) {
            for (int i = 0; i < 26; i++) {
                int px = 420 + r.nextInt(460), py = 20 + r.nextInt(400);
                int sz = 6 + r.nextInt(12);
                g.setColor(withAlpha(new Color(0xFFD700), 120 + r.nextInt(100)));
                g.fill(new Ellipse2D.Float(px, py, sz, sz));
            }
            trophyBig(g, WIDTH - 130, 70, 70);
        }
        if (l.has("fx-stars")) {
            for (int i = 0; i < 34; i++) {
                int px = r.nextInt(WIDTH), py = r.nextInt(HEIGHT);
                star(g, px, py, 5 + r.nextInt(10), 2 + r.nextInt(4), 4, withAlpha(new Color(0xFFF3B0), 120 + r.nextInt(120)));
            }
        }
    }

    // ----------------------------------------------------------- icon shapes

    private static void star(Graphics2D g, int cx, int cy, int outer, int inner, int points, Color c) {
        Polygon p = new Polygon();
        for (int i = 0; i < points * 2; i++) {
            double a = -Math.PI / 2 + i * Math.PI / points;
            double rad = i % 2 == 0 ? outer : inner;
            p.addPoint((int) Math.round(cx + Math.cos(a) * rad), (int) Math.round(cy + Math.sin(a) * rad));
        }
        g.setColor(c);
        g.fill(p);
    }

    private static void medal(Graphics2D g, int cx, int cy, int r, Color c) {
        g.setColor(new Color(0xD62828));
        Polygon ribbon = new Polygon(new int[]{cx - 10, cx - 2, cx - 6}, new int[]{cy - r + 4, cy - r + 4, cy + 2}, 3);
        g.fill(ribbon);
        g.setColor(new Color(0x1D3557));
        g.fill(new Polygon(new int[]{cx + 2, cx + 10, cx + 6}, new int[]{cy - r + 4, cy - r + 4, cy + 2}, 3));
        g.setColor(c);
        g.fill(new Ellipse2D.Float(cx - r + 4, cy - r + 10, (r - 4) * 2, (r - 4) * 2));
        g.setColor(new Color(255, 255, 255, 160));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Float(cx - r + 8, cy - r + 14, (r - 8) * 2, (r - 8) * 2));
        star(g, cx, cy + 6, 8, 3, 5, new Color(255, 255, 255, 220));
    }

    private static void trophy(Graphics2D g, int cx, int cy, int r) {
        g.setColor(new Color(0xFFC300));
        Path2D cup = new Path2D.Float();
        cup.moveTo(cx - r + 6, cy - r + 6);
        cup.lineTo(cx + r - 6, cy - r + 6);
        cup.curveTo(cx + r - 6, cy + 6, cx + 6, cy + 8, cx, cy + 8);
        cup.curveTo(cx - 6, cy + 8, cx - r + 6, cy + 6, cx - r + 6, cy - r + 6);
        g.fill(cup);
        g.setStroke(new BasicStroke(3f));
        g.draw(new java.awt.geom.Arc2D.Float(cx - r - 2, cy - r + 8, 14, 16, 90, 180, java.awt.geom.Arc2D.OPEN));
        g.draw(new java.awt.geom.Arc2D.Float(cx + r - 12, cy - r + 8, 14, 16, 270, 180, java.awt.geom.Arc2D.OPEN));
        g.fillRect(cx - 3, cy + 8, 6, 8);
        g.fill(new RoundRectangle2D.Float(cx - 12, cy + 16, 24, 6, 3, 3));
    }

    private static void trophyBig(Graphics2D g, int cx, int cy, int size) {
        java.awt.geom.AffineTransform at = g.getTransform();
        g.translate(cx, cy);
        g.scale(size / 22.0, size / 22.0);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
        trophy(g, 0, 0, 22);
        g.setComposite(AlphaComposite.SrcOver);
        g.setTransform(at);
    }

    private static void puzzle(Graphics2D g, int cx, int cy, int r) {
        g.setColor(new Color(0x2EC4B6));
        g.fill(new RoundRectangle2D.Float(cx - r + 4, cy - r + 4, (r - 4) * 2, (r - 4) * 2, 8, 8));
        g.fill(new Ellipse2D.Float(cx + r - 10, cy - 7, 14, 14));
        g.fill(new Ellipse2D.Float(cx - 7, cy - r - 2, 14, 14));
        g.setColor(new Color(0, 0, 0, 150));
        g.fill(new Ellipse2D.Float(cx - r + 1, cy - 6, 12, 12));
    }

    private static void drawFlag(Graphics2D g, int x, int y, int h, Color c) {
        g.setColor(new Color(255, 255, 255, 230));
        g.fillRect(x, y, 3, h);
        g.setColor(c);
        Path2D f = new Path2D.Float();
        f.moveTo(x + 3, y);
        f.lineTo(x + 3 + h * 0.8, y + h * 0.2);
        f.lineTo(x + 3, y + h * 0.5);
        f.closePath();
        g.fill(f);
    }

    // --------------------------------------------------------------- helpers

    private static Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    private static String fit(Graphics2D g, String text, int maxWidth) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) return text;
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t + "…") > maxWidth) t = t.substring(0, t.length() - 1);
        return t + "…";
    }
}

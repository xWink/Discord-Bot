package command.util.misc;

import command.util.art.Sticker;
import economy.Cosmetics;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws the profile card in the server's red and gold. The mascot on the right wears whatever the player has
 * equipped, so every cosmetic is visible as art. Everything is drawn with shapes and the bundled mascot, so the
 * card looks the same on any machine.
 */
public final class ProfileCard {

    public static final int WIDTH = 900;
    public static final int HEIGHT = 440;

    private static final Color GOLD = Sticker.GOLD;
    private static final Color INK = Sticker.INK;

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
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            draw(g, s, loadout, seed);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    // ----------------------------------------------------------------- draw

    private static void draw(Graphics2D g, Stats s, Cosmetics.Loadout l, long seed) throws IOException {
        boolean bundle = l.has("bundle-weekend");
        boolean glow = l.has("flair-day");
        Color top = bundle ? new Color(0xFF2A2A) : new Color(0xA30000);
        Color bottom = bundle ? new Color(0x8A0000) : new Color(0x2E0000);
        Shape card = new RoundRectangle2D.Float(8, 8, WIDTH - 16, HEIGHT - 16, 36, 36);

        g.setPaint(new GradientPaint(0, 0, top, WIDTH, HEIGHT, bottom));
        g.fill(card);
        g.setClip(card);
        g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 46), 0, HEIGHT * 0.55f, new Color(255, 255, 255, 0)));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // soft spotlight behind the mascot
        g.setPaint(new java.awt.RadialGradientPaint(720, 190, 260, new float[]{0f, 1f},
                new Color[]{new Color(255, 221, 0, 120), new Color(255, 221, 0, 0)}));
        g.fillRect(440, 0, 460, HEIGHT);
        g.setClip(null);

        if (glow) {
            for (int i = 10; i >= 1; i--) {
                g.setColor(new Color(255, 221, 0, 16));
                g.setStroke(new BasicStroke(i * 2.4f));
                g.draw(card);
            }
        }
        g.setColor(GOLD);
        g.setStroke(new BasicStroke(6f));
        g.draw(card);
        g.setColor(INK);
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Float(2, 2, WIDTH - 4, HEIGHT - 4, 42, 42));
        if (bundle) {
            g.setColor(GOLD);
            g.setStroke(new BasicStroke(2.5f));
            g.draw(new RoundRectangle2D.Float(24, 24, WIDTH - 48, HEIGHT - 48, 26, 26));
        }

        // mascot, wearing the loadout
        BufferedImage mascot = Sticker.render(l.codes, 400, seed);
        g.drawImage(mascot, 510, 22, 390, 390, null);

        drawAvatar(g, s, 44, 44, 128);
        drawNameBlock(g, s, l, 196);
        drawStats(g, s, 44, 214);
        drawWearing(g, l, 44, HEIGHT - 66);

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(255, 221, 0, 170));
        String tag = "GRYPHCOIN";
        g.drawString(tag, WIDTH - 40 - g.getFontMetrics().stringWidth(tag), HEIGHT - 34);
    }

    private static void drawAvatar(Graphics2D g, Stats s, int x, int y, int size) {
        Shape circle = new Ellipse2D.Float(x, y, size, size);
        g.setColor(new Color(0, 0, 0, 110));
        g.fill(new Ellipse2D.Float(x + 4, y + 6, size, size));
        Shape old = g.getClip();
        g.setClip(circle);
        if (s.avatar != null) {
            g.drawImage(s.avatar, x, y, size, size, null);
        } else {
            g.setPaint(new GradientPaint(x, y, new Color(0xFFE766), x + size, y + size, new Color(0xE0A800)));
            g.fill(circle);
            g.setFont(new Font("SansSerif", Font.BOLD, 62));
            g.setColor(INK);
            String initial = s.name.isEmpty() ? "?" : s.name.substring(0, 1).toUpperCase();
            FontMetrics fm = g.getFontMetrics();
            g.drawString(initial, x + (size - fm.stringWidth(initial)) / 2, y + (size + fm.getAscent() - fm.getDescent()) / 2);
        }
        g.setClip(old);
        g.setColor(INK);
        g.setStroke(new BasicStroke(9f));
        g.draw(circle);
        g.setColor(GOLD);
        g.setStroke(new BasicStroke(5f));
        g.draw(circle);
    }

    private static void drawNameBlock(Graphics2D g, Stats s, Cosmetics.Loadout l, int x) {
        g.setFont(new Font("SansSerif", Font.BOLD, 40));
        String name = fit(g, s.name, 480 - x + 60);
        // outlined name: dark stroke first, white fill on top
        java.awt.font.GlyphVector gv = g.getFont().createGlyphVector(g.getFontRenderContext(), name);
        Shape outline = gv.getOutline(x, 92);
        g.setColor(INK);
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(outline);
        g.setColor(Color.WHITE);
        g.fill(outline);

        int y = 110;
        if (l.has("bundle-weekend") || l.has("title-week")) {
            ribbon(g, x, y, l.has("bundle-weekend") ? "the Weekender" : "the Scholar");
            y += 46;
        }
        if (l.has("banner-club")) {
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            String team = fit(g, l.team != null ? l.team : "Team", 260) + " banner";
            g.setColor(INK);
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            java.awt.font.GlyphVector tv = g.getFont().createGlyphVector(g.getFontRenderContext(), team);
            Shape to = tv.getOutline(x + 2, y + 22);
            g.draw(to);
            g.setColor(GOLD);
            g.fill(to);
        }
    }

    private static void ribbon(Graphics2D g, int x, int y, String text) {
        g.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, 22));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text) + 40;
        Path2D p = new Path2D.Float();
        p.moveTo(x, y);
        p.lineTo(x + w + 14, y);
        p.lineTo(x + w, y + 17);
        p.lineTo(x + w + 14, y + 34);
        p.lineTo(x, y + 34);
        p.closePath();
        g.setColor(GOLD);
        g.fill(p);
        g.setColor(INK);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(p);
        g.drawString(text, x + 18, y + 25);
    }

    private static void drawStats(Graphics2D g, Stats s, int x, int y) {
        String[][] cells = {
                {"BANGS", Integer.toString(s.bangs)},
                {"DEATHS", Integer.toString(s.deaths)},
                {"JAMS", Integer.toString(s.jams)},
                {"SURVIVAL", String.format("%.1f%%", s.survival)},
                {"STREAK", Integer.toString(s.streak)},
                {"KARMA", Integer.toString(s.karma)},
                {"UNLOCKS", s.achievements + "/" + s.achievementsTotal},
        };
        int cellW = 104;
        int cellH = 66;
        int gap = 8;
        for (int i = 0; i < cells.length; i++) {
            int cx = x + (i % 4) * (cellW + gap);
            int cy = y + (i / 4) * (cellH + gap);
            Shape box = new RoundRectangle2D.Float(cx, cy, cellW, cellH, 16, 16);
            g.setColor(new Color(0, 0, 0, 120));
            g.fill(box);
            g.setColor(INK);
            g.setStroke(new BasicStroke(3f));
            g.draw(box);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(255, 221, 0, 220));
            g.drawString(cells[i][0], cx + 12, cy + 22);
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            g.setColor(Color.WHITE);
            g.drawString(cells[i][1], cx + 12, cy + 52);
        }
    }

    private static void drawWearing(Graphics2D g, Cosmetics.Loadout l, int x, int y) {
        List<String> names = new ArrayList<>();
        for (String code : l.codes) names.add(Cosmetics.label(code));
        g.setFont(new Font("SansSerif", names.isEmpty() ? Font.ITALIC : Font.BOLD, 13));
        g.setColor(new Color(255, 255, 255, names.isEmpty() ? 150 : 225));
        if (names.isEmpty()) {
            g.drawString("Nothing equipped yet. Try !shop and !equip.", x, y);
            return;
        }
        // wrap onto a second line instead of cutting items off
        FontMetrics fm = g.getFontMetrics();
        StringBuilder line = new StringBuilder("Wearing: ");
        int row = 0;
        for (int i = 0; i < names.size(); i++) {
            String piece = names.get(i) + (i < names.size() - 1 ? "  \u2022  " : "");
            if (fm.stringWidth(line + piece) > 470 && line.length() > 9) {
                g.drawString(line.toString(), x, y + row * 18);
                row++;
                line = new StringBuilder();
            }
            line.append(piece);
        }
        g.drawString(line.toString(), x, y + row * 18);
    }

    private static String fit(Graphics2D g, String text, int maxWidth) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) return text;
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t + "…") > maxWidth) t = t.substring(0, t.length() - 1);
        return t + "…";
    }
}

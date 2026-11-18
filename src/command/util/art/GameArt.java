package command.util.art;

import command.util.game.SlotsGame;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Pictures and animations for the games: the bang cylinder, the spinning coin, tumbling dice and slot reels.
 * Same look as the profile card (slate card, gold border) and drawn entirely with shapes plus the bundled
 * mascot and coin art, so it renders the same on any machine.
 */
public final class GameArt {

    private static final Color GOLD = Sticker.GOLD;
    private static final Color INK = Sticker.INK;
    private static final Color RED = new Color(0xE02020);

    private GameArt() { }

    // ------------------------------------------------------------------ shared

    private static BufferedImage canvas(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            hints(g);
            Shape card = new RoundRectangle2D.Float(6, 6, w - 12, h - 12, 30, 30);
            g.setPaint(new GradientPaint(0, 0, new Color(0x3A3F47), w, h, new Color(0x14161A)));
            g.fill(card);
            g.setClip(card);
            g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 40), 0, h * 0.5f, new Color(255, 255, 255, 0)));
            g.fillRect(0, 0, w, h);
            g.setClip(null);
            g.setColor(GOLD);
            g.setStroke(new BasicStroke(5f));
            g.draw(card);
            g.setColor(INK);
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 34, 34));
        } finally {
            g.dispose();
        }
        return img;
    }

    private static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    /** Text with a dark outline, centred on {@code cx}. */
    private static void outlined(Graphics2D g, String text, Font font, int cx, int baseline, Color fill) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        java.awt.font.GlyphVector gv = font.createGlyphVector(g.getFontRenderContext(), text);
        Shape outline = gv.getOutline(cx - fm.stringWidth(text) / 2f, baseline);
        g.setColor(INK);
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(outline);
        g.setColor(fill);
        g.fill(outline);
    }

    private static void centered(Graphics2D g, String text, Font font, Color color, int cx, int baseline) {
        g.setFont(font);
        g.setColor(color);
        g.drawString(text, cx - g.getFontMetrics().stringWidth(text) / 2, baseline);
    }

    private static byte[] png(BufferedImage img) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static BufferedImage mascot;

    private static synchronized BufferedImage mascot() throws IOException {
        if (mascot == null) mascot = Sticker.render(new ArrayList<String>(), 300, 0);
        return mascot;
    }

    // -------------------------------------------------------------------- bang

    /** What happened on a pull. */
    public enum Pull { CLICK, BANG, JAM }

    /**
     * The result card for one trigger pull.
     *
     * @param name       the player's name
     * @param pullIndex  which chamber was pulled, 0 to 5 (the earlier ones were clicks)
     * @param result     what happened
     * @param streak     the player's current daily streak
     * @param codes      cosmetics the mascot wears
     */
    public static byte[] bangCard(String name, int pullIndex, Pull result, int streak, Collection<String> codes,
                                  long seed) throws IOException {
        int w = 700;
        int h = 300;
        BufferedImage img = canvas(w, h);
        Graphics2D g = img.createGraphics();
        try {
            hints(g);
            String head = result == Pull.BANG ? "BANG!" : result == Pull.JAM ? "JAMMED!" : "CLICK";
            Color headColor = result == Pull.BANG ? RED : result == Pull.JAM ? new Color(0x4CD964) : Color.WHITE;
            outlined(g, head, new Font("SansSerif", Font.BOLD, 58), 220, 92, headColor);
            String sub = result == Pull.BANG ? name + " did not make it"
                    : result == Pull.JAM ? name + " got lucky" : name + " survived";
            centered(g, fitText(g, sub, new Font("SansSerif", Font.BOLD, 20), 380), new Font("SansSerif", Font.BOLD, 20),
                    new Color(255, 255, 255, 220), 220, 128);

            // the cylinder: spent chambers are grey, the pulled one shows the result, the rest are unknown
            for (int i = 0; i < 6; i++) {
                int cx = 52 + i * 64;
                int cy = 188;
                Ellipse2D slot = new Ellipse2D.Float(cx - 26, cy - 26, 52, 52);
                Color fill = new Color(0, 0, 0, 150);
                String mark = "";
                if (i < pullIndex) {
                    fill = new Color(110, 115, 125);
                    mark = "✓";
                } else if (i == pullIndex) {
                    fill = result == Pull.BANG ? RED : result == Pull.JAM ? new Color(0x4CD964) : GOLD;
                    mark = result == Pull.BANG ? "✕" : result == Pull.JAM ? "♣" : "✓";
                } else {
                    mark = "?";
                }
                g.setColor(fill);
                g.fill(slot);
                g.setColor(i == pullIndex ? Color.WHITE : INK);
                g.setStroke(new BasicStroke(i == pullIndex ? 4f : 3f));
                g.draw(slot);
                centered(g, mark, new Font("SansSerif", Font.BOLD, 24), i == pullIndex ? INK : new Color(255, 255, 255, 170),
                        cx, cy + 9);
            }
            centered(g, "Chamber " + (pullIndex + 1) + " of 6", new Font("SansSerif", Font.PLAIN, 13),
                    new Color(255, 255, 255, 150), 52 + 5 * 32, 238);

            // streak
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(new Color(255, 221, 0, 230));
            int toBonus = 10 - (streak % 10);
            g.drawString(streak > 0 ? "STREAK " + streak + "  ·  bonus in " + toBonus + (toBonus == 1 ? " day" : " days")
                    : "Pull daily to build a streak", 40, 276);

            // the mascot: greyed and tipped over if the pull was fatal
            BufferedImage m = Sticker.render(codes, 230, seed);
            if (result == Pull.BANG) {
                BufferedImage grey = new BufferedImage(m.getWidth(), m.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = grey.createGraphics();
                gg.drawImage(m, 0, 0, null);
                gg.dispose();
                new ColorConvertOp(java.awt.color.ColorSpace.getInstance(java.awt.color.ColorSpace.CS_GRAY), null)
                        .filter(grey, grey);
                // ColorConvertOp drops alpha on some JVMs, so restore it from the original
                for (int y = 0; y < m.getHeight(); y++) {
                    for (int x = 0; x < m.getWidth(); x++) {
                        int a = m.getRGB(x, y) >>> 24;
                        grey.setRGB(x, y, (a << 24) | (grey.getRGB(x, y) & 0xFFFFFF));
                    }
                }
                AffineTransform old = g.getTransform();
                g.rotate(Math.toRadians(-14), 560, 150);
                g.drawImage(grey, 445, 35, null);
                g.setTransform(old);
            } else {
                g.drawImage(m, 445, 35, null);
            }
            if (result == Pull.JAM) {
                for (int i = 0; i < 3; i++) clover(g, 470 + i * 70, 40 + (i % 2) * 20, 22);
            }
        } finally {
            g.dispose();
        }
        return png(img);
    }

    private static String fitText(Graphics2D g, String text, Font font, int max) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t) > max) t = t.substring(0, t.length() - 1);
        return t.equals(text) ? t : t + "…";
    }

    private static void clover(Graphics2D g, int cx, int cy, int r) {
        g.setColor(new Color(0x2FBF4F));
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2 * i + Math.PI / 4;
            g.fill(new Ellipse2D.Double(cx + Math.cos(a) * r * 0.5 - r * 0.5, cy + Math.sin(a) * r * 0.5 - r * 0.5, r, r));
        }
        g.setColor(INK);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(cx, cy, cx + r / 2, cy + r + 4);
    }

    // -------------------------------------------------------------------- coin

    private static final int COIN_FRAMES = 14;

    /**
     * A coin that spins, slows and lands on the given face.
     *
     * @return a GIF that plays once and rests on the result
     */
    public static byte[] coinSpin(boolean heads) throws IOException {
        BufferedImage face = ImageIO.read(GameArt.class.getClassLoader().getResourceAsStream("loonie_heads.png"));
        BufferedImage back = ImageIO.read(GameArt.class.getClassLoader().getResourceAsStream("loonie_tails.png"));
        int halfTurns = heads ? 6 : 7;
        int w = 320;
        int h = 280;
        BufferedImage bg = canvas(w, h);
        List<BufferedImage> frames = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        for (int i = 0; i < COIN_FRAMES; i++) {
            double p = i / (double) (COIN_FRAMES - 1);
            double eased = 1 - Math.pow(1 - p, 2.2);
            double angle = Math.PI * halfTurns * eased;
            double cos = Math.cos(angle);
            // the coin shows heads while it faces up, and the back once it has turned past edge-on
            boolean showHeads = cos >= 0;
            double scale = i == COIN_FRAMES - 1 ? 1 : Math.max(0.08, Math.abs(cos));
            double hop = i == COIN_FRAMES - 1 ? 0 : Math.sin(Math.PI * p) * 46;
            BufferedImage frame = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frame.createGraphics();
            try {
                hints(g);
                g.drawImage(bg, 0, 0, null);
                // soft shadow on the ground
                g.setColor(new Color(0, 0, 0, (int) (90 - hop)));
                g.fill(new Ellipse2D.Double(w / 2.0 - 60 + hop * 0.4, 222, 120 - hop * 0.8, 16));
                BufferedImage src = showHeads ? face : back;
                int size = 170;
                int dh = (int) Math.max(6, size * scale);
                g.drawImage(src, (w - size) / 2, (int) (h / 2.0 - dh / 2.0 - hop + 4), size, dh, null);
            } finally {
                g.dispose();
            }
            frames.add(frame);
            delays.add(i == COIN_FRAMES - 1 ? 3000 : 60 + (int) (60 * p));
        }
        return GifWriter.write(frames, delays, false);
    }

    // -------------------------------------------------------------------- dice

    private static void die(Graphics2D g, int x, int y, int size, int value, double tilt) {
        AffineTransform old = g.getTransform();
        g.rotate(tilt, x + size / 2.0, y + size / 2.0);
        Shape body = new RoundRectangle2D.Float(x, y, size, size, size * 0.22f, size * 0.22f);
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new RoundRectangle2D.Float(x + 4, y + 6, size, size, size * 0.22f, size * 0.22f));
        g.setPaint(new GradientPaint(x, y, Color.WHITE, x + size, y + size, new Color(0xD8D8D8)));
        g.fill(body);
        g.setColor(INK);
        g.setStroke(new BasicStroke(4f));
        g.draw(body);
        g.setColor(INK);
        double[][] pos = {{0.5, 0.5}, {0.25, 0.25}, {0.75, 0.75}, {0.25, 0.75}, {0.75, 0.25}, {0.25, 0.5}, {0.75, 0.5}};
        int[][] pips = {{}, {0}, {1, 2}, {0, 1, 2}, {1, 2, 3, 4}, {0, 1, 2, 3, 4}, {1, 2, 3, 4, 5, 6}};
        int r = size / 9;
        for (int idx : pips[value]) {
            g.fill(new Ellipse2D.Double(x + pos[idx][0] * size - r, y + pos[idx][1] * size - r, r * 2, r * 2));
        }
        g.setTransform(old);
    }

    /** Two dice that tumble and settle on the roll. */
    public static byte[] diceRoll(int a, int b) throws IOException {
        int w = 360;
        int h = 220;
        BufferedImage bg = canvas(w, h);
        Random rnd = new Random(a * 31L + b);
        List<BufferedImage> frames = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        int rolls = 7;
        for (int i = 0; i <= rolls; i++) {
            boolean last = i == rolls;
            int va = last ? a : 1 + rnd.nextInt(6);
            int vb = last ? b : 1 + rnd.nextInt(6);
            BufferedImage frame = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frame.createGraphics();
            try {
                hints(g);
                g.drawImage(bg, 0, 0, null);
                double settle = last ? 0 : 1 - i / (double) rolls;
                die(g, 55, 55 + (int) (rnd.nextInt(30) * settle), 108, va, (rnd.nextDouble() - 0.5) * 1.2 * settle);
                die(g, 197, 55 + (int) (rnd.nextInt(30) * settle), 108, vb, (rnd.nextDouble() - 0.5) * 1.2 * settle);
                if (last) {
                    outlined(g, "= " + (a + b), new Font("SansSerif", Font.BOLD, 30), w / 2, 196, Color.WHITE);
                }
            } finally {
                g.dispose();
            }
            frames.add(frame);
            delays.add(last ? 3000 : 90 + i * 15);
        }
        return GifWriter.write(frames, delays, false);
    }

    // ------------------------------------------------------------------- slots

    private static void symbol(Graphics2D g, SlotsGame.Symbol s, double cx, double cy, double size) throws IOException {
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        double k = size / 100.0;
        g.scale(k, k);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (s) {
            case CHERRY: {
                g.setColor(new Color(0x2FA84F));
                g.draw(new Path2D.Double(new java.awt.geom.QuadCurve2D.Double(-18, 10, -10, -40, 14, -42)));
                g.draw(new Path2D.Double(new java.awt.geom.QuadCurve2D.Double(18, 14, 14, -30, 14, -42)));
                for (int sx : new int[]{-20, 20}) {
                    Ellipse2D c = new Ellipse2D.Double(sx - 20, 4, 40, 40);
                    g.setColor(new Color(0xE01C2C));
                    g.fill(c);
                    g.setColor(INK);
                    g.draw(c);
                    g.setColor(new Color(255, 255, 255, 170));
                    g.fill(new Ellipse2D.Double(sx - 10, 12, 9, 9));
                }
                break;
            }
            case LEMON: {
                Ellipse2D e = new Ellipse2D.Double(-44, -28, 88, 56);
                g.setPaint(new GradientPaint(-30, -20, new Color(0xFFF06A), 30, 20, new Color(0xF2C200)));
                g.fill(e);
                g.setColor(INK);
                g.draw(e);
                g.setColor(new Color(255, 255, 255, 150));
                g.fill(new Ellipse2D.Double(-26, -16, 22, 10));
                break;
            }
            case BELL: {
                Path2D bell = new Path2D.Double();
                bell.moveTo(-40, 30);
                bell.curveTo(-34, 24, -30, 4, -28, -14);
                bell.curveTo(-24, -42, 24, -42, 28, -14);
                bell.curveTo(30, 4, 34, 24, 40, 30);
                bell.closePath();
                g.setPaint(new GradientPaint(-30, -30, new Color(0xFFE766), 30, 30, new Color(0xE0A800)));
                g.fill(bell);
                g.setColor(INK);
                g.draw(bell);
                g.setColor(INK);
                g.fill(new Ellipse2D.Double(-9, 28, 18, 18));
                break;
            }
            case STAR: {
                Path2D star = new Path2D.Double();
                for (int i = 0; i < 10; i++) {
                    double a = Math.PI / 5 * i - Math.PI / 2;
                    double rad = i % 2 == 0 ? 46 : 20;
                    double x = Math.cos(a) * rad;
                    double y = Math.sin(a) * rad;
                    if (i == 0) star.moveTo(x, y);
                    else star.lineTo(x, y);
                }
                star.closePath();
                g.setPaint(new GradientPaint(-30, -30, new Color(0xFFF07A), 30, 30, new Color(0xFFC400)));
                g.fill(star);
                g.setColor(INK);
                g.draw(star);
                break;
            }
            case SEVEN: {
                Font f = new Font("SansSerif", Font.BOLD, 100);
                java.awt.font.GlyphVector gv = f.createGlyphVector(g.getFontRenderContext(), "7");
                Shape o = gv.getOutline(-26, 36);
                g.setColor(INK);
                g.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(o);
                g.setColor(RED);
                g.fill(o);
                break;
            }
            default: {
                g.drawImage(mascot(), -50, -50, 100, 100, null);
            }
        }
        g.setTransform(old);
    }

    private static final int REEL = 108;

    /**
     * Three reels that spin and stop one after another.
     *
     * @param reels the final three symbols
     * @param label what to show under the reels at the end, for example "WIN x2.5" (may be empty)
     * @param win   true to flash the reels gold at the end
     */
    public static byte[] slots(SlotsGame.Symbol[] reels, String label, boolean win) throws IOException {
        int w = 440;
        int h = 250;
        BufferedImage bg = canvas(w, h);
        SlotsGame.Symbol[] all = SlotsGame.Symbol.values();
        int total = 20;
        int[] stop = {9, 13, 17};
        Random rnd = new Random(reels[0].ordinal() * 7L + reels[1].ordinal() * 13L + reels[2].ordinal());
        List<BufferedImage> frames = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        for (int f = 0; f < total; f++) {
            boolean last = f == total - 1;
            BufferedImage frame = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frame.createGraphics();
            try {
                hints(g);
                g.drawImage(bg, 0, 0, null);
                for (int r = 0; r < 3; r++) {
                    int x = 38 + r * (REEL + 20);
                    int y = 44;
                    RoundRectangle2D window = new RoundRectangle2D.Float(x, y, REEL, REEL, 20, 20);
                    g.setColor(new Color(0xF4F4F0));
                    g.fill(window);
                    Shape oldClip = g.getClip();
                    g.setClip(window);
                    if (f >= stop[r]) {
                        symbol(g, reels[r], x + REEL / 2.0, y + REEL / 2.0, REEL * 0.8);
                    } else {
                        // a blur of symbols sliding down the window
                        double slide = (f * 47 + r * 31) % REEL;
                        SlotsGame.Symbol a = all[(f * 3 + r * 2 + rnd.nextInt(2)) % all.length];
                        SlotsGame.Symbol b = all[(f * 3 + r * 2 + 3) % all.length];
                        symbol(g, a, x + REEL / 2.0, y + slide, REEL * 0.7);
                        symbol(g, b, x + REEL / 2.0, y + slide - REEL, REEL * 0.7);
                        g.setPaint(new GradientPaint(0, y, new Color(0, 0, 0, 60), 0, y + REEL / 2f, new Color(0, 0, 0, 0)));
                        g.fillRect(x, y, REEL, REEL / 2);
                    }
                    g.setClip(oldClip);
                    g.setColor(last && win ? GOLD : INK);
                    g.setStroke(new BasicStroke(last && win ? 7f : 5f));
                    g.draw(window);
                }
                if (last && !label.isEmpty()) {
                    outlined(g, label, new Font("SansSerif", Font.BOLD, 30), w / 2, 214, win ? GOLD : Color.WHITE);
                }
            } finally {
                g.dispose();
            }
            frames.add(frame);
            delays.add(last ? 3500 : 70 + (f >= 8 ? 30 : 0));
        }
        return GifWriter.write(frames, delays, false);
    }
}

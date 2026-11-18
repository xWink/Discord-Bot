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

    /**
     * Remembers the animations that come out identical every time (the coin has two outcomes, the dice 36, the wheel
     * 37), so each is drawn once. The bytes are shared and never changed; the size is capped so memory stays small.
     */
    private static final class Memo {
        private final java.util.Map<Integer, byte[]> map;

        Memo(int max) {
            map = new java.util.LinkedHashMap<Integer, byte[]>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<Integer, byte[]> eldest) {
                    return size() > max;
                }
            };
        }

        byte[] get(int key, java.util.concurrent.Callable<byte[]> draw) throws IOException {
            synchronized (map) {
                byte[] hit = map.get(key);
                if (hit != null) return hit;
            }
            byte[] made;
            try {
                made = draw.call();
            } catch (IOException | RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
            synchronized (map) {
                map.put(key, made);
            }
            return made;
        }
    }

    private static final Memo COINS = new Memo(2);
    private static final Memo DICE = new Memo(36);
    private static final Memo WHEELS = new Memo(12);

    // ------------------------------------------------------------------ shared

    static BufferedImage canvas(int w, int h) {
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

    static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    /** Text with a dark outline, centred on {@code cx}. */
    static void outlined(Graphics2D g, String text, Font font, int cx, int baseline, Color fill) {
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

    static void centered(Graphics2D g, String text, Font font, Color color, int cx, int baseline) {
        g.setFont(font);
        g.setColor(color);
        g.drawString(text, cx - g.getFontMetrics().stringWidth(text) / 2, baseline);
    }

    static byte[] png(BufferedImage img) throws IOException {
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

    private static final int BANG_W = 700;
    private static final int BANG_H = 300;

    /** A revolver cylinder seen end-on, turned by {@code angle}, with a marker at the top where the hammer falls. */
    private static void cylinder(Graphics2D g, double cx, double cy, double r, double angle) {
        Ellipse2D body = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
        g.setPaint(new GradientPaint((float) (cx - r), (float) (cy - r), new Color(0x9AA0AA), (float) (cx + r),
                (float) (cy + r), new Color(0x3C4048)));
        g.fill(body);
        g.setColor(INK);
        g.setStroke(new BasicStroke(6f));
        g.draw(body);
        for (int i = 0; i < 6; i++) {
            double a = angle + Math.PI / 3 * i - Math.PI / 2;
            double hx = cx + Math.cos(a) * r * 0.6;
            double hy = cy + Math.sin(a) * r * 0.6;
            Ellipse2D hole = new Ellipse2D.Double(hx - r * 0.2, hy - r * 0.2, r * 0.4, r * 0.4);
            g.setColor(new Color(0x15171B));
            g.fill(hole);
            g.setColor(INK);
            g.setStroke(new BasicStroke(3f));
            g.draw(hole);
        }
        g.setColor(new Color(0x6A707A));
        g.fill(new Ellipse2D.Double(cx - r * 0.18, cy - r * 0.18, r * 0.36, r * 0.36));
        g.setColor(INK);
        g.draw(new Ellipse2D.Double(cx - r * 0.18, cy - r * 0.18, r * 0.36, r * 0.36));
        // the marker where the hammer falls
        Path2D tip = new Path2D.Double();
        tip.moveTo(cx - 12, cy - r - 20);
        tip.lineTo(cx + 12, cy - r - 20);
        tip.lineTo(cx, cy - r - 2);
        tip.closePath();
        g.setColor(GOLD);
        g.fill(tip);
        g.setColor(INK);
        g.setStroke(new BasicStroke(3f));
        g.draw(tip);
    }

    /** A burst of rays and a glow, for the shot or a sparkle of luck. */
    private static void burst(Graphics2D g, double cx, double cy, double r, Color core, Color rays) {
        g.setPaint(new java.awt.RadialGradientPaint((float) cx, (float) cy, (float) r, new float[]{0f, 1f},
                new Color[]{core, new Color(core.getRed(), core.getGreen(), core.getBlue(), 0)}));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setColor(rays);
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 12; i++) {
            double a = Math.PI / 6 * i;
            double in = r * (i % 2 == 0 ? 0.35 : 0.45);
            double out = r * (i % 2 == 0 ? 1.0 : 0.75);
            g.draw(new java.awt.geom.Line2D.Double(cx + Math.cos(a) * in, cy + Math.sin(a) * in,
                    cx + Math.cos(a) * out, cy + Math.sin(a) * out));
        }
    }

    /**
     * The pull as an animation: the cylinder spins and slows, the hammer waits, then the shot, a click or a jam
     * lands and the animation settles on the result card.
     *
     * @return a GIF that plays once and rests on {@link #bangCard}
     */
    public static byte[] bangAnimation(String name, int pullIndex, Pull result, int streak, Collection<String> codes,
                                       long seed) throws IOException {
        BufferedImage bg = canvas(BANG_W, BANG_H);
        BufferedImage m = Sticker.render(codes, 230, seed);
        BufferedImage finalCard = ImageIO.read(new java.io.ByteArrayInputStream(
                bangCard(name, pullIndex, result, streak, codes, seed)));
        List<BufferedImage> frames = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();

        int spin = 9;
        for (int i = 0; i < spin; i++) {
            double p = i / (double) (spin - 1);
            double angle = Math.PI * 2 * 2.2 * (1 - Math.pow(1 - p, 2));
            BufferedImage f = copy(bg);
            Graphics2D g = f.createGraphics();
            try {
                hints(g);
                cylinder(g, 220, 160, 100, angle);
                outlined(g, "spinning\u2026", new Font("SansSerif", Font.BOLD, 22), 220, 285, Color.WHITE);
                g.drawImage(m, 445, 35, null);
            } finally {
                g.dispose();
            }
            frames.add(f);
            delays.add(60 + (int) (70 * p));
        }
        // the cylinder stops and the hammer waits
        for (int i = 0; i < 2; i++) {
            BufferedImage f = copy(bg);
            Graphics2D g = f.createGraphics();
            try {
                hints(g);
                cylinder(g, 220, 160, 100, 0);
                outlined(g, i == 0 ? "\u2026" : "\u2026\u2026", new Font("SansSerif", Font.BOLD, 30), 220, 285, Color.WHITE);
                int shake = i == 1 ? 3 : 0;
                g.drawImage(m, 445 + shake, 35, null);
            } finally {
                g.dispose();
            }
            frames.add(f);
            delays.add(i == 0 ? 450 : 350);
        }
        // the moment itself
        int flashes = result == Pull.BANG ? 3 : 2;
        for (int i = 0; i < flashes; i++) {
            BufferedImage f = copy(bg);
            Graphics2D g = f.createGraphics();
            try {
                hints(g);
                cylinder(g, 220, 160, 100, 0);
                double grow = 0.6 + 0.4 * i;
                if (result == Pull.BANG) {
                    burst(g, 220, 160, 150 * grow, new Color(255, 240, 160, 230), new Color(255, 120, 0, 230));
                    g.setColor(new Color(255, 60, 40, 70 - i * 20));
                    g.fillRect(0, 0, BANG_W, BANG_H);
                    int dx = (i % 2 == 0 ? 7 : -7);
                    g.drawImage(m, 445 + dx, 35, null);
                    outlined(g, "BANG!", new Font("SansSerif", Font.BOLD, 56), 350, 150, RED);
                } else if (result == Pull.JAM) {
                    burst(g, 220, 160, 110 * grow, new Color(120, 255, 150, 200), new Color(60, 200, 90, 220));
                    g.drawImage(m, 445, 35, null);
                    outlined(g, "jam!", new Font("SansSerif", Font.BOLD, 44), 350, 150, new Color(0x4CD964));
                } else {
                    burst(g, 220, 70, 36 * grow, new Color(255, 255, 255, 220), new Color(255, 221, 0, 230));
                    g.drawImage(m, 445, 35, null);
                    outlined(g, "click", new Font("SansSerif", Font.BOLD, 40), 350, 150, Color.WHITE);
                }
            } finally {
                g.dispose();
            }
            frames.add(f);
            delays.add(result == Pull.BANG ? 90 : 140);
        }
        frames.add(finalCard);
        delays.add(5000);
        return GifWriter.write(frames, delays, false);
    }

    static BufferedImage copy(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
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
        return COINS.get(heads ? 1 : 0, () -> drawCoin(heads));
    }

    private static byte[] drawCoin(boolean heads) throws IOException {
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
        return DICE.get(a * 10 + b, () -> drawDice(a, b));
    }

    private static byte[] drawDice(int a, int b) throws IOException {
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

    // ---------------------------------------------------------------- roulette

    private static Color pocketColor(int number) {
        if (number == 0) return new Color(0x1F9D45);
        return command.util.game.RouletteGame.isRed(number) ? new Color(0xC81E1E) : new Color(0x1A1A1A);
    }

    private static void wheel(Graphics2D g, double cx, double cy, double r, double rotation) {
        int[] order = command.util.game.RouletteGame.WHEEL;
        double step = Math.PI * 2 / order.length;
        g.setColor(new Color(0x5B3A1E));
        g.fill(new Ellipse2D.Double(cx - r - 12, cy - r - 12, (r + 12) * 2, (r + 12) * 2));
        g.setColor(INK);
        g.setStroke(new BasicStroke(5f));
        g.draw(new Ellipse2D.Double(cx - r - 12, cy - r - 12, (r + 12) * 2, (r + 12) * 2));
        for (int i = 0; i < order.length; i++) {
            double a0 = rotation + step * i - step / 2 - Math.PI / 2;
            g.setColor(pocketColor(order[i]));
            g.fill(new java.awt.geom.Arc2D.Double(cx - r, cy - r, r * 2, r * 2, -Math.toDegrees(a0),
                    -Math.toDegrees(step), java.awt.geom.Arc2D.PIE));
        }
        g.setColor(new Color(0x15171B));
        g.fill(new Ellipse2D.Double(cx - r * 0.62, cy - r * 0.62, r * 1.24, r * 1.24));
        g.setColor(GOLD);
        g.setStroke(new BasicStroke(3f));
        g.draw(new Ellipse2D.Double(cx - r * 0.62, cy - r * 0.62, r * 1.24, r * 1.24));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(Color.WHITE);
        for (int i = 0; i < order.length; i++) {
            double a = rotation + step * i - Math.PI / 2;
            AffineTransform old = g.getTransform();
            g.translate(cx + Math.cos(a) * r * 0.81, cy + Math.sin(a) * r * 0.81);
            g.rotate(a + Math.PI / 2);
            String t = Integer.toString(order[i]);
            g.drawString(t, -g.getFontMetrics().stringWidth(t) / 2f, 4);
            g.setTransform(old);
        }
        g.setPaint(new java.awt.RadialGradientPaint((float) cx, (float) cy, (float) (r * 0.3), new float[]{0f, 1f},
                new Color[]{new Color(0xFFE766), new Color(0xB8860B)}));
        g.fill(new Ellipse2D.Double(cx - r * 0.3, cy - r * 0.3, r * 0.6, r * 0.6));
    }

    /** The wheel spins and slows until the ball sits in the result's pocket at the top; the number then lights up. */
    public static byte[] roulette(int result) throws IOException {
        return WHEELS.get(result, () -> drawRoulette(result));
    }

    private static byte[] drawRoulette(int result) throws IOException {
        int w = 520;
        int h = 320;
        BufferedImage bg = canvas(w, h);
        int frames = 18;
        double finalRotation = -Math.PI * 2 / 37 * command.util.game.RouletteGame.pocket(result);
        List<BufferedImage> out = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        for (int i = 0; i < frames; i++) {
            double p = i / (double) (frames - 1);
            double eased = 1 - Math.pow(1 - p, 3);
            double rotation = finalRotation - Math.PI * 2 * 3 * (1 - eased);
            BufferedImage f = copy(bg);
            Graphics2D g = f.createGraphics();
            try {
                hints(g);
                wheel(g, 160, 160, 120, rotation);
                // the ball rides the rim at the top
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(160 - 7, 160 - 120 - 9, 14, 14));
                g.setColor(INK);
                g.setStroke(new BasicStroke(2f));
                g.draw(new Ellipse2D.Double(160 - 7, 160 - 120 - 9, 14, 14));
                if (i == frames - 1) {
                    g.setColor(pocketColor(result));
                    g.fill(new Ellipse2D.Double(340, 70, 140, 140));
                    g.setColor(GOLD);
                    g.setStroke(new BasicStroke(7f));
                    g.draw(new Ellipse2D.Double(340, 70, 140, 140));
                    outlined(g, Integer.toString(result), new Font("SansSerif", Font.BOLD, 64), 410, 142 + 22, Color.WHITE);
                    String colour = result == 0 ? "GREEN" : command.util.game.RouletteGame.isRed(result) ? "RED" : "BLACK";
                    centered(g, colour, new Font("SansSerif", Font.BOLD, 20), GOLD, 410, 245);
                } else {
                    centered(g, "no more bets", new Font("SansSerif", Font.BOLD, 18), new Color(255, 255, 255, 160), 410, 150);
                }
            } finally {
                g.dispose();
            }
            out.add(f);
            delays.add(i == frames - 1 ? 4000 : 55 + (int) (95 * p * p));
        }
        return GifWriter.write(out, delays, false);
    }

    // ------------------------------------------------------------------- crash

    /**
     * The multiplier climbing as a curve. It rises to the crash point; the target is marked with a dot that turns
     * green if the climb gets there in time. Ends on the result.
     */
    public static byte[] crash(int crashPoint, int target, boolean won) throws IOException {
        int w = 520;
        int h = 320;
        BufferedImage bg = canvas(w, h);
        double end = crashPoint / 100.0;
        double tgt = target / 100.0;
        double top = Math.max(2.0, Math.max(end, tgt) * 1.25);
        int px = 40;
        int py = 40;
        int pw = w - 80;
        int ph = h - 100;
        int frames = 20;
        List<BufferedImage> out = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        for (int i = 0; i < frames; i++) {
            double p = (i + 1) / (double) frames;
            double m = Math.exp(Math.log(end) * p); // climbs fast at first on a log scale, ends at the crash point
            boolean last = i == frames - 1;
            BufferedImage f = copy(bg);
            Graphics2D g = f.createGraphics();
            try {
                hints(g);
                g.setColor(new Color(255, 255, 255, 30));
                g.setStroke(new BasicStroke(1.5f));
                for (int k = 1; k <= 4; k++) g.drawLine(px, py + ph * k / 4, px + pw, py + ph * k / 4);
                // target line
                double ty = py + ph - ph * (tgt - 1) / (top - 1);
                g.setColor(new Color(255, 221, 0, 140));
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{8f, 8f}, 0));
                g.draw(new java.awt.geom.Line2D.Double(px, ty, px + pw, ty));
                g.setFont(new Font("SansSerif", Font.BOLD, 13));
                g.setColor(GOLD);
                g.drawString("target " + CrashGame_show(target), px + 4, (int) ty - 6);
                // the curve
                Path2D curve = new Path2D.Double();
                int steps = 60;
                double lastX = px;
                double lastY = py + ph;
                for (int k = 0; k <= steps; k++) {
                    double q = p * k / steps;
                    double mm = Math.exp(Math.log(end) * q);
                    double x = px + pw * q;
                    double y = py + ph - ph * (mm - 1) / (top - 1);
                    if (k == 0) curve.moveTo(x, y);
                    else curve.lineTo(x, y);
                    lastX = x;
                    lastY = y;
                }
                boolean crashed = last && !won;
                g.setColor(crashed ? RED : new Color(0x4CD964));
                g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(curve);
                if (m >= tgt || (last && won)) {
                    double q = Math.log(tgt) / Math.log(end); // how far along the climb the target sits
                    double dx = px + pw * Math.min(1, q);
                    g.setColor(new Color(0x4CD964));
                    g.fill(new Ellipse2D.Double(dx - 9, ty - 9, 18, 18));
                    g.setColor(INK);
                    g.setStroke(new BasicStroke(3f));
                    g.draw(new Ellipse2D.Double(dx - 9, ty - 9, 18, 18));
                }
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(lastX - 7, lastY - 7, 14, 14));
                String label = last ? (won ? "CASHED OUT " + CrashGame_show(target) + "  (crash " + CrashGame_show(crashPoint) + ")"
                        : "CRASHED at " + CrashGame_show(crashPoint)) : CrashGame_show((int) Math.round(m * 100));
                outlined(g, label, new Font("SansSerif", Font.BOLD, last ? 24 : 40), w / 2, last ? 293 : 296,
                        last ? (won ? new Color(0x4CD964) : RED) : Color.WHITE);
            } finally {
                g.dispose();
            }
            out.add(f);
            delays.add(last ? 4000 : 90);
        }
        return GifWriter.write(out, delays, false);
    }

    private static String CrashGame_show(int hundredths) {
        return command.util.game.CrashGame.show(hundredths);
    }

    // ------------------------------------------------------------------- mines

    /**
     * The 4x4 board when a round ends: gems on the safe tiles that were found, every mine shown, and the one that
     * went off marked with a burst.
     *
     * @param found    tiles the player uncovered
     * @param mines    where the mines were
     * @param exploded the tile that blew up, or -1 if the player cashed out
     */
    public static byte[] minesBoard(java.util.Set<Integer> found, java.util.Set<Integer> mines, int exploded,
                                    String label, boolean won) throws IOException {
        int w = 420;
        int h = 400;
        BufferedImage img = canvas(w, h);
        Graphics2D g = img.createGraphics();
        try {
            hints(g);
            int cell = 78;
            int gap = 10;
            int x0 = (w - (4 * cell + 3 * gap)) / 2;
            for (int i = 0; i < 16; i++) {
                int x = x0 + (i % 4) * (cell + gap);
                int y = 26 + (i / 4) * (cell + gap);
                RoundRectangle2D tile = new RoundRectangle2D.Float(x, y, cell, cell, 16, 16);
                boolean isMine = mines.contains(i);
                boolean isFound = found.contains(i);
                g.setColor(i == exploded ? new Color(0x8A1010) : isFound ? new Color(0x1E5B3A) : new Color(0, 0, 0, 130));
                g.fill(tile);
                g.setColor(INK);
                g.setStroke(new BasicStroke(3f));
                g.draw(tile);
                double cx = x + cell / 2.0;
                double cy = y + cell / 2.0;
                if (i == exploded) burst(g, cx, cy, 42, new Color(255, 240, 160, 220), new Color(255, 120, 0, 230));
                if (isMine) {
                    g.setColor(new Color(0x2B2F36));
                    g.fill(new Ellipse2D.Double(cx - 17, cy - 17, 34, 34));
                    g.setColor(INK);
                    g.setStroke(new BasicStroke(3f));
                    g.draw(new Ellipse2D.Double(cx - 17, cy - 17, 34, 34));
                    g.setColor(new Color(255, 255, 255, 140));
                    g.fill(new Ellipse2D.Double(cx - 9, cy - 11, 9, 9));
                    g.setColor(new Color(255, 255, 255, 210));
                    for (int k = 0; k < 8; k++) {
                        double a = Math.PI / 4 * k;
                        g.drawLine((int) (cx + Math.cos(a) * 17), (int) (cy + Math.sin(a) * 17),
                                (int) (cx + Math.cos(a) * 25), (int) (cy + Math.sin(a) * 25));
                    }
                } else if (isFound) {
                    Path2D gem = new Path2D.Double();
                    gem.moveTo(cx, cy - 24);
                    gem.lineTo(cx + 22, cy - 4);
                    gem.lineTo(cx, cy + 24);
                    gem.lineTo(cx - 22, cy - 4);
                    gem.closePath();
                    g.setPaint(new GradientPaint((float) (cx - 20), (float) (cy - 20), new Color(0x9CF5FF),
                            (float) (cx + 20), (float) (cy + 20), new Color(0x23A8D8)));
                    g.fill(gem);
                    g.setColor(INK);
                    g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(gem);
                }
            }
            outlined(g, label, new Font("SansSerif", Font.BOLD, 26), w / 2, h - 18, won ? new Color(0x4CD964) : RED);
        } finally {
            g.dispose();
        }
        return png(img);
    }

    // ----------------------------------------------------------------- scratch

    /**
     * A 3x3 scratch card: silver cells are scratched off in turn to show the symbols, and the three that match
     * (if any) light up.
     */
    public static byte[] scratch(command.util.game.ScratchGame.Card card, String label, boolean won) throws IOException {
        int w = 440;
        int h = 470;
        BufferedImage bg = canvas(w, h);
        int cell = 104;
        int gap = 12;
        int x0 = (w - (3 * cell + 2 * gap)) / 2;
        int order[] = {4, 0, 8, 2, 6, 1, 7, 3, 5};
        List<BufferedImage> out = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        for (int step = 0; step <= 9; step++) {
            boolean last = step == 9;
            BufferedImage f = copy(bg);
            Graphics2D g = f.createGraphics();
            try {
                hints(g);
                outlined(g, "SCRATCH CARD", new Font("SansSerif", Font.BOLD, 26), w / 2, 40, GOLD);
                for (int i = 0; i < 9; i++) {
                    int x = x0 + (i % 3) * (cell + gap);
                    int y = 62 + (i / 3) * (cell + gap);
                    RoundRectangle2D tile = new RoundRectangle2D.Float(x, y, cell, cell, 18, 18);
                    boolean open = false;
                    for (int k = 0; k < step; k++) if (order[k] == i) open = true;
                    boolean partial = step < 9 && order[step] == i;
                    boolean hit = last && card.prize != null && card.cells[i] == card.prize.symbol;
                    if (open) {
                        g.setColor(hit ? new Color(0xFFF3B0) : new Color(0xF4F4F0));
                        g.fill(tile);
                        symbol(g, card.cells[i], x + cell / 2.0, y + cell / 2.0, cell * 0.78);
                    } else {
                        g.setPaint(new GradientPaint(x, y, new Color(0xC9CDD3), x + cell, y + cell, new Color(0x8C919A)));
                        g.fill(tile);
                        if (partial) {
                            // half scratched: a diagonal swipe of the symbol shows through
                            Shape old = g.getClip();
                            g.setClip(tile);
                            g.setColor(new Color(0xF4F4F0));
                            g.fill(new java.awt.geom.Rectangle2D.Double(x, y, cell * 0.5, cell));
                            symbol(g, card.cells[i], x + cell / 2.0, y + cell / 2.0, cell * 0.78);
                            g.setColor(new Color(0x9A9FA8));
                            g.fill(new java.awt.geom.Rectangle2D.Double(x + cell * 0.5, y, cell * 0.5, cell));
                            g.setClip(old);
                        } else {
                            centered(g, "?", new Font("SansSerif", Font.BOLD, 40), new Color(255, 255, 255, 170), x + cell / 2,
                                    y + cell / 2 + 14);
                        }
                    }
                    g.setColor(hit ? GOLD : INK);
                    g.setStroke(new BasicStroke(hit ? 7f : 4f));
                    g.draw(tile);
                }
                if (last) outlined(g, label, new Font("SansSerif", Font.BOLD, 28), w / 2, h - 20, won ? GOLD : Color.WHITE);
            } finally {
                g.dispose();
            }
            out.add(f);
            delays.add(last ? 4000 : 260);
        }
        return GifWriter.write(out, delays, false);
    }

    // ------------------------------------------------------------------ wordle

    private static final Color WORDLE_GREEN = new Color(0x3FA34D);
    private static final Color WORDLE_YELLOW = new Color(0xD4A017);
    private static final Color WORDLE_GREY = new Color(0x4A4F57);

    private static void wordleTile(Graphics2D g, int x, int y, int size, char letter, int score, double squash) {
        double h = Math.max(4, size * squash);
        double top = y + (size - h) / 2;
        RoundRectangle2D tile = new RoundRectangle2D.Double(x, top, size, h, 12, 12);
        if (score < 0) {
            g.setColor(new Color(0, 0, 0, 90));
            g.fill(tile);
            g.setColor(letter == 0 ? new Color(255, 255, 255, 50) : new Color(255, 255, 255, 140));
            g.setStroke(new BasicStroke(3f));
            g.draw(tile);
        } else {
            g.setColor(score == 2 ? WORDLE_GREEN : score == 1 ? WORDLE_YELLOW : WORDLE_GREY);
            g.fill(tile);
            g.setColor(INK);
            g.setStroke(new BasicStroke(3f));
            g.draw(tile);
        }
        if (letter != 0 && squash > 0.45) {
            Font f = new Font("SansSerif", Font.BOLD, (int) (size * 0.55));
            g.setFont(f);
            g.setColor(Color.WHITE);
            String t = Character.toString(Character.toUpperCase(letter));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(t, x + (size - fm.stringWidth(t)) / 2f, (float) (top + h / 2 + fm.getAscent() * 0.36));
        }
    }

    private static BufferedImage wordleFrame(BufferedImage bg, List<String> guesses, String answer, int revealed,
                                             double flip, String footer) {
        int size = 62;
        int gap = 8;
        int x0 = (bg.getWidth() - (5 * size + 4 * gap)) / 2;
        BufferedImage f = copy(bg);
        Graphics2D g = f.createGraphics();
        try {
            hints(g);
            outlined(g, "WORDLE", new Font("SansSerif", Font.BOLD, 28), bg.getWidth() / 2, 42, GOLD);
            for (int row = 0; row < 6; row++) {
                String guess = row < guesses.size() ? guesses.get(row) : null;
                int[] score = guess == null ? null : command.util.game.WordleGame.score(guess, answer);
                boolean newest = row == guesses.size() - 1;
                for (int col = 0; col < 5; col++) {
                    int x = x0 + col * (size + gap);
                    int y = 62 + row * (size + gap);
                    if (guess == null) {
                        wordleTile(g, x, y, size, (char) 0, -1, 1);
                    } else if (newest && col > revealed) {
                        wordleTile(g, x, y, size, guess.charAt(col), -1, 1); // typed but not revealed yet
                    } else if (newest && col == revealed) {
                        // mid-flip: the letter squashes away, then comes back coloured
                        wordleTile(g, x, y, size, guess.charAt(col), flip < 0.5 ? -1 : score[col],
                                flip < 0.5 ? 1 - flip * 2 : (flip - 0.5) * 2);
                    } else {
                        wordleTile(g, x, y, size, guess.charAt(col), score[col], 1);
                    }
                }
            }
            if (footer != null) {
                centered(g, footer, new Font("SansSerif", Font.BOLD, 18), new Color(255, 255, 255, 220), bg.getWidth() / 2,
                        bg.getHeight() - 20);
            }
        } finally {
            g.dispose();
        }
        return f;
    }

    /** The board as a picture. */
    public static byte[] wordleBoard(List<String> guesses, String answer, String footer) throws IOException {
        return png(wordleFrame(canvas(420, 62 + 6 * 70 + 30), guesses, answer, 99, 1, footer));
    }

    /** The board with the newest row flipping over tile by tile to show its colours. */
    public static byte[] wordleReveal(List<String> guesses, String answer, String footer) throws IOException {
        BufferedImage bg = canvas(420, 62 + 6 * 70 + 30);
        List<BufferedImage> frames = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        for (int col = 0; col < 5; col++) {
            for (int step = 0; step < 3; step++) {
                frames.add(wordleFrame(bg, guesses, answer, col, step / 2.0, null));
                delays.add(step == 2 ? 120 : 90);
            }
        }
        frames.add(wordleFrame(bg, guesses, answer, 99, 1, footer));
        delays.add(5000);
        return GifWriter.write(frames, delays, false);
    }
}

package command.util.art;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * The look of every picture: 8-bit arcade. Hard pixel edges with notched corners, two-pixel ink borders, a hard
 * offset shadow under each card, bevelled buttons, and Press Start 2P for type. All of it is neutral grey, in a dark
 * and a light {@link Theme}; the colour comes only from the game art itself.
 *
 * <p>Everything here draws crisp (no anti-aliasing) on whole pixels, and text sits on the font's 8-pixel grid.
 */
public final class Ui {

    /** Dark line work on the game's own art (symbols, dice pips), the same in both themes. */
    public static final Color OUTLINE = new Color(0x15151A);
    /** The light face of a reel, a die or a scratch cell, the same in both themes. */
    public static final Color PAPER = new Color(0xF4F4F6);

    /** First row below the card's title strip; pictures draw at or under this. */
    public static final int TOP = 28;
    /** How far the hard shadow reaches past the card on the right and bottom. */
    public static final int SHADOW = 4;

    private Ui() { }

    // ---------------------------------------------------------------- palette

    public static Color card() { return Theme.current().card; }
    public static Color inset() { return Theme.current().inset; }
    public static Color control() { return Theme.current().face; }
    public static Color select() { return Theme.current().select; }
    public static Color divider() { return Theme.current().divider; }
    public static Color text() { return Theme.current().text; }
    public static Color muted() { return Theme.current().muted; }
    public static Color faint() { return Theme.current().faint; }
    public static Color ink() { return Theme.current().ink; }
    public static Color ok() { return Theme.current().ok; }
    public static Color bad() { return Theme.current().bad; }

    // ------------------------------------------------------------------- type

    private static Font base;
    private static boolean baseTried;

    private static synchronized Font base() {
        if (!baseTried) {
            baseTried = true;
            try (InputStream in = Ui.class.getClassLoader().getResourceAsStream("fonts/PressStart2P-Regular.ttf")) {
                if (in != null) base = Font.createFont(Font.TRUETYPE_FONT, in);
            } catch (Exception | Error e) {
                base = null; // fall back to a monospaced face; the pictures still draw
            }
        }
        return base;
    }

    /**
     * The pixel font at the size that best matches {@code size} in points of ordinary type. Press Start 2P is wide and
     * only sharp on multiples of 8, so sizes snap to 8, 16, 24 and so on.
     */
    public static Font bold(int size) {
        int px = Math.max(8, (int) Math.round(size * 0.6 / 8.0) * 8);
        return pixel(px);
    }

    public static Font plain(int size) {
        return bold(size);
    }

    /** The pixel font at exactly {@code px} (use multiples of 8). */
    public static Font pixel(int px) {
        Font f = base();
        return f == null ? new Font(Font.MONOSPACED, Font.BOLD, px) : f.deriveFont((float) px);
    }

    /** Sets the hints every picture wants: crisp text, smooth shapes and scaling for the game art. */
    public static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    // ----------------------------------------------------------------- pixels

    private static Object crisp(Graphics2D g) {
        Object old = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        return old;
    }

    private static void restore(Graphics2D g, Object old) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old == null ? RenderingHints.VALUE_ANTIALIAS_DEFAULT : old);
    }

    /** A filled box with a two-pixel border and the four corner pixels notched away. */
    private static void notched(Graphics2D g, int x, int y, int w, int h, Color fill, Color border) {
        g.setColor(fill);
        g.fillRect(x + 2, y + 2, w - 4, h - 4);
        g.setColor(border);
        g.fillRect(x + 2, y, w - 4, 2);
        g.fillRect(x + 2, y + h - 2, w - 4, 2);
        g.fillRect(x, y + 2, 2, h - 4);
        g.fillRect(x + w - 2, y + 2, 2, h - 4);
        // the notch: a one-pixel diagonal step so the corner reads as round, not cut
        g.fillRect(x + 2, y + 2, 2, 2);
        g.fillRect(x + w - 4, y + 2, 2, 2);
        g.fillRect(x + 2, y + h - 4, 2, 2);
        g.fillRect(x + w - 4, y + h - 4, 2, 2);
    }

    /** A button or tile: a raised face, light on the top and left edge, dark on the bottom and right. */
    public static void control(Graphics2D g, int x, int y, int w, int h) {
        Theme t = Theme.current();
        face(g, x, y, w, h, t.face, t.faceHi, t.faceLo, false);
    }

    /** Same, in a colour of the caller's choosing (the first colour is ignored; kept so old call sites read the same). */
    public static void control(Graphics2D g, int x, int y, int w, int h, Color ignored, Color colour) {
        Theme t = Theme.current();
        face(g, x, y, w, h, colour, mix(colour, Color.WHITE, 0.25), mix(colour, Color.BLACK, 0.3), false);
    }

    private static void face(Graphics2D g, int x, int y, int w, int h, Color fill, Color hi, Color lo, boolean pressed) {
        Object old = crisp(g);
        Theme t = Theme.current();
        notched(g, x, y, w, h, fill, t.ink);
        Color top = pressed ? lo : hi;
        Color bottom = pressed ? hi : lo;
        g.setColor(top);
        g.fillRect(x + 4, y + 2, w - 8, 2);
        g.fillRect(x + 2, y + 4, 2, h - 8);
        g.setColor(bottom);
        g.fillRect(x + 4, y + h - 4, w - 8, 2);
        g.fillRect(x + w - 4, y + 4, 2, h - 8);
        restore(g, old);
    }

    /** A recessed field in the given colour: ink border and a darker top and left edge. */
    public static void inset(Graphics2D g, int x, int y, int w, int h, Color fill) {
        Object old = crisp(g);
        Theme t = Theme.current();
        notched(g, x, y, w, h, fill, t.ink);
        g.setColor(mix(fill, Color.BLACK, 0.25));
        g.fillRect(x + 4, y + 2, w - 8, 2);
        g.fillRect(x + 2, y + 4, 2, h - 8);
        restore(g, old);
    }

    /** A tile with only a border, for something switched off. */
    public static void outline(Graphics2D g, int x, int y, int w, int h, Color edge) {
        Object old = crisp(g);
        notched(g, x, y, w, h, Theme.current().card, edge);
        restore(g, old);
    }

    /** A solid two-pixel frame just outside a tile, to mark the one that matters. */
    public static void ring(Graphics2D g, int x, int y, int w, int h) {
        Object old = crisp(g);
        g.setColor(Theme.current().text);
        frame(g, x - 6, y - 6, w + 12, h + 12, 0, 0);
        restore(g, old);
    }

    /** A dashed frame just outside a tile, for "nearly" or "a match". */
    public static void dashedRing(Graphics2D g, int x, int y, int w, int h) {
        Object old = crisp(g);
        g.setColor(Theme.current().muted);
        frame(g, x - 6, y - 6, w + 12, h + 12, 6, 4);
        restore(g, old);
    }

    private static void frame(Graphics2D g, int x, int y, int w, int h, int dash, int gap) {
        if (dash == 0) {
            g.fillRect(x, y, w, 2);
            g.fillRect(x, y + h - 2, w, 2);
            g.fillRect(x, y, 2, h);
            g.fillRect(x + w - 2, y, 2, h);
            return;
        }
        for (int i = 0; i < w; i += dash + gap) {
            int len = Math.min(dash, w - i);
            g.fillRect(x + i, y, len, 2);
            g.fillRect(x + i, y + h - 2, len, 2);
        }
        for (int i = 0; i < h; i += dash + gap) {
            int len = Math.min(dash, h - i);
            g.fillRect(x, y + i, 2, len);
            g.fillRect(x + w - 2, y + i, 2, len);
        }
    }

    /** A dotted line between sections. */
    public static void divider(Graphics2D g, int x1, int x2, int y) {
        Object old = crisp(g);
        g.setColor(Theme.current().divider);
        for (int x = x1; x < x2; x += 8) g.fillRect(x, y, 4, 2);
        restore(g, old);
    }

    public static Color mix(Color a, Color b, double f) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * f), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * f),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * f));
    }

    // ------------------------------------------------------------------- text

    public static void centred(Graphics2D g, String s, Font font, Color color, int cx, int baseline) {
        g.setFont(font);
        g.setColor(color);
        g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, baseline);
    }

    /** A button with a caption. {@code on} presses it in and fills it with the selected colour. */
    public static void button(Graphics2D g, int x, int y, int w, int h, String caption, Font font, boolean on) {
        Theme t = Theme.current();
        if (on) {
            Object old = crisp(g);
            face(g, x, y, w, h, t.select, mix(t.select, Color.WHITE, 0.2), mix(t.select, Color.BLACK, 0.25), true);
            restore(g, old);
        } else {
            control(g, x, y, w, h);
        }
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(t.text);
        g.drawString(caption, x + (w - fm.stringWidth(caption)) / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2 + (on ? 2 : 0));
    }

    // ------------------------------------------------------------------- card

    /**
     * An empty card: a notched box with a hard shadow, a title strip along the top and the body below it. The
     * shadow uses the last {@link #SHADOW} pixels on the right and bottom, so content should stay inside the rest.
     */
    public static BufferedImage card(int w, int h, String title) {
        Theme t = Theme.current();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            Object old = crisp(g);
            int cw = w - SHADOW;
            int ch = h - SHADOW;
            g.setColor(t.ink);
            g.fillRect(SHADOW + 2, SHADOW + 2, cw - 4, ch - 4);
            g.fillRect(SHADOW + 4, SHADOW, cw - 8, ch);
            g.fillRect(SHADOW, SHADOW + 4, cw, ch - 8);
            notched(g, 0, 0, cw, ch, t.card, t.ink);
            // the title strip
            g.setColor(t.face);
            g.fillRect(2, 2, cw - 4, 22);
            g.setColor(t.faceHi);
            g.fillRect(4, 2, cw - 8, 2);
            g.setColor(t.ink);
            g.fillRect(2, 24, cw - 4, 2);
            g.setColor(t.card);
            g.fillRect(2, 2, 2, 2);
            g.fillRect(cw - 4, 2, 2, 2);
            g.setColor(t.ink);
            g.fillRect(2, 2, 2, 2);
            g.fillRect(cw - 4, 2, 2, 2);
            restore(g, old);
            g.setFont(pixel(8));
            FontMetrics fm = g.getFontMetrics();
            String s = title;
            while (s.length() > 1 && fm.stringWidth(s) > cw - 40) s = s.substring(0, s.length() - 1);
            if (!s.equals(title)) s = s.trim() + "..";
            g.setColor(t.muted);
            g.drawString(s, 12, 18);
        } finally {
            g.dispose();
        }
        return img;
    }
}

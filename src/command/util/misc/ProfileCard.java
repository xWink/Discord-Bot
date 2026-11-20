package command.util.misc;

import command.util.art.GifWriter;
import command.util.art.Ui;
import command.util.art.Sticker;
import economy.Cosmetics;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws the profile card as a plain grey Swing window: a title bar, bevelled fields and buttons. The mascot on the right wears whatever the player has
 * equipped, so every cosmetic is visible as art. Everything is drawn with shapes and the bundled mascot, so the
 * card looks the same on any machine.
 */
public final class ProfileCard {

    public static final int WIDTH = 900;
    /** Height of the card with no inventory panel. */
    public static final int HEIGHT = 440;

    /** The sunken box the mascot sits in. */
    private static final java.awt.Rectangle WELL = new java.awt.Rectangle(496, 34, 396, 392);

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
        /** Labels for the colour rental and installment plan, shown before the cosmetics. */
        public List<String> extras = new ArrayList<>();
    }

    public static byte[] render(Stats s, Cosmetics.Loadout loadout, long seed) throws IOException {
        return render(s, loadout, java.util.Collections.<Cosmetics.OwnedItem>emptyList(), seed);
    }

    public static byte[] render(Stats s, Cosmetics.Loadout loadout, List<Cosmetics.OwnedItem> owned, long seed)
            throws IOException {
        System.setProperty("java.awt.headless", "true");
        // measure the inventory first so the card can grow to fit it
        BufferedImage probe = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = probe.createGraphics();
        List<Pill> pills = layoutPills(pg, owned, loadout, s.extras);
        pg.dispose();
        int height = HEIGHT + panelHeight(pills);
        BufferedImage img = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            Ui.hints(g);
            draw(g, s, loadout, pills, height, seed);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    // ------------------------------------------------------------- animated

    /** Frames in the animated card. With {@link #FRAME_MS} this loops about once a second. */
    static final int FRAMES = 12;
    static final int FRAME_MS = 80;
    /** Discord allows 10 MB for most uploads; stay well under so slow links and busy servers cope. */
    static final int MAX_GIF_BYTES = 6 * 1024 * 1024;

    /**
     * Same card as {@link #render}, as a looping GIF: the mascot bobs and stars twinkle around it.
     *
     * @return the GIF, or null if it came out too large to send (callers fall back to the still card)
     */
    public static byte[] renderAnimated(Stats s, Cosmetics.Loadout loadout, List<Cosmetics.OwnedItem> owned, long seed)
            throws IOException {
        System.setProperty("java.awt.headless", "true");
        BufferedImage probe = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = probe.createGraphics();
        List<Pill> pills = layoutPills(pg, owned, loadout, s.extras);
        pg.dispose();
        int height = HEIGHT + panelHeight(pills);
        BufferedImage mascot = Sticker.render(loadout.codes, 380, seed);
        java.util.Random random = new java.util.Random(seed);
        double[][] stars = new double[9][];
        for (int i = 0; i < stars.length; i++) {
            // x, y, size, phase
            stars[i] = new double[]{520 + random.nextInt(350), 56 + random.nextInt(340), 6 + random.nextInt(7),
                    random.nextDouble()};
        }

        List<BufferedImage> frames = new ArrayList<>();
        for (int i = 0; i < FRAMES; i++) {
            double t = i / (double) FRAMES;
            BufferedImage img = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            try {
                Ui.hints(g);
                drawBase(g, s, loadout, height);
                int bob = (int) Math.round(5 * Math.sin(2 * Math.PI * t));
                g.setClip(WELL.x + 2, WELL.y + 2, WELL.width - 4, WELL.height - 4);
                g.drawImage(mascot, 504, 40 + bob, 380, 380, null);
                g.setClip(null);
                drawFront(g, s, loadout, pills, height);
                for (double[] star : stars) {
                    double glow = Math.max(0, Math.sin(2 * Math.PI * (t + star[3])));
                    if (glow > 0.15) twinkle(g, star[0], star[1], star[2] * (0.5 + glow), (int) (60 + 195 * glow));
                }
            } finally {
                g.dispose();
            }
            frames.add(img);
        }
        byte[] bytes = GifWriter.write(frames, FRAME_MS, true);
        return bytes.length > MAX_GIF_BYTES ? null : bytes;
    }

    /** A four-point star, for the twinkle. */
    private static void twinkle(Graphics2D g, double cx, double cy, double r, int alpha) {
        Path2D star = new Path2D.Double();
        double in = r * 0.28;
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i - Math.PI / 2;
            double rad = i % 2 == 0 ? r : in;
            double x = cx + Math.cos(a) * rad;
            double y = cy + Math.sin(a) * rad;
            if (i == 0) star.moveTo(x, y);
            else star.lineTo(x, y);
        }
        star.closePath();
        g.setColor(new Color(0x6B, 0x6B, 0x6B, Math.min(255, alpha)));
        g.fill(star);
    }

    // ----------------------------------------------------------------- draw

    private static void draw(Graphics2D g, Stats s, Cosmetics.Loadout l, List<Pill> pills, int height, long seed)
            throws IOException {
        drawBase(g, s, l, height);
        // mascot, wearing the loadout
        BufferedImage mascot = Sticker.render(l.codes, 380, seed);
        g.setClip(WELL.x + 2, WELL.y + 2, WELL.width - 4, WELL.height - 4);
        g.drawImage(mascot, 504, 40, 380, 380, null);
        g.setClip(null);
        drawFront(g, s, l, pills, height);
    }

    /** The window, and the empty well the mascot goes in. */
    private static void drawBase(Graphics2D g, Stats s, Cosmetics.Loadout l, int height) {
        BufferedImage window = Ui.card(WIDTH, height, "Profile");
        g.drawImage(window, 0, 0, null);
        Ui.inset(g, WELL.x, WELL.y, WELL.width, WELL.height, Ui.inset());
        // a dotted focus ring stands in for the glow of the day flair
        if (l.has("flair-day")) Ui.dashedRing(g, WELL.x - 3, WELL.y - 3, WELL.width + 6, WELL.height + 6);
        // an extra etched frame stands in for the weekend bundle's second border
        if (l.has("bundle-weekend")) {
            Ui.divider(g, 12, WIDTH - 12, height - 10);
            Ui.divider(g, 12, WIDTH - 12, Ui.TOP + 4);
        }
    }

    /** Everything in front of the mascot: avatar, name, stats, inventory. */
    private static void drawFront(Graphics2D g, Stats s, Cosmetics.Loadout l, List<Pill> pills, int height) {
        drawAvatar(g, s, 44, 44, 128);
        drawNameBlock(g, s, l, 196);
        drawStats(g, s, 44, 226);
        drawInventory(g, pills, l, 44, HEIGHT - 24);
        drawWearing(g, l, 44, 388);
    }

    /** One line naming what the mascot has on, so the art is never a guessing game. */
    private static void drawWearing(Graphics2D g, Cosmetics.Loadout l, int x, int y) {
        if (l.codes.isEmpty()) return;
        List<String> names = new ArrayList<>();
        for (String code : l.codes) names.add(Cosmetics.label(code));
        g.setFont(Ui.bold(13));
        g.setColor(Ui.text());
        g.drawString("Wearing:", x, y);
        g.setFont(Ui.plain(13));
        g.setColor(Ui.muted());
        g.drawString(fit(g, String.join(", ", names), 400 - 80), x + 70, y);
    }

    private static void drawAvatar(Graphics2D g, Stats s, int x, int y, int size) {
        Shape circle = new java.awt.geom.Ellipse2D.Float(x, y, size, size);
        Shape old = g.getClip();
        g.setClip(circle);
        if (s.avatar != null) {
            g.drawImage(s.avatar, x, y, size, size, null);
        } else {
            g.setColor(Ui.control());
            g.fillRect(x, y, size, size);
            g.setFont(Ui.bold(62));
            g.setColor(Ui.text());
            String initial = s.name.isEmpty() ? "?" : s.name.substring(0, 1).toUpperCase();
            FontMetrics fm = g.getFontMetrics();
            g.drawString(initial, x + (size - fm.stringWidth(initial)) / 2, y + (size + fm.getAscent() - fm.getDescent()) / 2);
        }
        g.setClip(old);
    }

    private static void drawNameBlock(Graphics2D g, Stats s, Cosmetics.Loadout l, int x) {
        g.setFont(Ui.bold(34));
        g.setColor(Ui.text());
        g.drawString(fit(g, s.name, 480 - x + 60), x, 90);

        int y = 104;
        if (l.alumni) {
            ribbon(g, x, y, "Alumni" + (l.alumniYear > 0 ? ", class of " + l.alumniYear : ""));
            y += 34;
        }
        if (l.has("bundle-weekend") || l.has("title-week")) {
            ribbon(g, x, y, l.has("bundle-weekend") ? "the Weekender" : "the Scholar");
            y += 34;
        }
        if (l.has("banner-club")) {
            g.setFont(Ui.bold(16));
            g.setColor(Ui.muted());
            g.drawString(fit(g, l.team != null ? l.team : "Team", 260) + " banner", x + 2, y + 18);
        }
    }

    /** A title shown on a small button face. */
    private static void ribbon(Graphics2D g, int x, int y, String text) {
        Font font = Ui.bold(15);
        g.setFont(font);
        int w = g.getFontMetrics().stringWidth(text) + 28;
        Ui.button(g, x, y, w, 28, text, font, false);
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
        int cellW = 104;
        int cellH = 66;
        int gap = 8;
        for (int i = 0; i < cells.length; i++) {
            int cx = x + (i % 4) * (cellW + gap);
            int cy = y + (i / 4) * (cellH + gap);
            // a caption over a read-only text field
            g.setFont(Ui.bold(12));
            g.setColor(Ui.muted());
            g.drawString(cells[i][0], cx + 2, cy + 12);
            Ui.inset(g, cx, cy + 18, cellW, cellH - 18, Ui.inset());
            Font value = Ui.plain(24);
            g.setFont(value);
            // shrink until the value fits inside the box, so "100.0%" never clips
            while (g.getFontMetrics().stringWidth(cells[i][1]) > cellW - 20 && value.getSize() > 12) {
                value = value.deriveFont((float) value.getSize() - 1);
                g.setFont(value);
            }
            g.setColor(Ui.text());
            g.drawString(cells[i][1], cx + 8, cy + 18 + (cellH - 18) / 2 + g.getFontMetrics().getAscent() / 2 - 2);
        }
    }

    // ------------------------------------------------------------ inventory

    /** One item chip in the inventory panel. */
    private static final class Pill {
        String text;
        boolean worn;
        boolean special;
        int x, y, w;
    }

    private static final int PILL_H = 30;
    private static final int PILL_GAP = 10;
    private static final int PANEL_LEFT = 44;
    private static final int PANEL_RIGHT = WIDTH - 44;
    private static final int MAX_PILLS = 18;

    private static String timeLeft(long expiresAt) {
        if (expiresAt == 0) return "permanent";
        long ms = expiresAt - System.currentTimeMillis();
        long days = ms / 86_400_000L;
        if (days >= 1) return days + "d left";
        long hours = ms / 3_600_000L;
        return hours >= 1 ? hours + "h left" : "<1h left";
    }

    private static List<Pill> layoutPills(Graphics2D g, List<Cosmetics.OwnedItem> owned, Cosmetics.Loadout worn,
                                          List<String> extras) {
        List<Pill> pills = new ArrayList<>();
        if (owned.isEmpty() && extras.isEmpty()) return pills;
        List<Cosmetics.OwnedItem> sorted = new ArrayList<>(owned);
        sorted.sort((a, b) -> {
            boolean wa = worn.codes.contains(a.code), wb = worn.codes.contains(b.code);
            if (wa != wb) return wa ? -1 : 1;
            return Cosmetics.label(a.code).compareToIgnoreCase(Cosmetics.label(b.code));
        });
        g.setFont(Ui.bold(14));
        FontMetrics fm = g.getFontMetrics();
        int x = PANEL_LEFT;
        int y = 0;
        for (String text : extras) {
            Pill p = new Pill();
            p.special = true;
            p.text = text;
            p.w = fm.stringWidth(text) + 28;
            if (x + p.w > PANEL_RIGHT) {
                x = PANEL_LEFT;
                y += PILL_H + PILL_GAP;
            }
            p.x = x;
            p.y = y;
            x += p.w + PILL_GAP;
            pills.add(p);
        }
        int shown = Math.min(sorted.size(), MAX_PILLS);
        for (int i = 0; i < shown; i++) {
            Cosmetics.OwnedItem item = sorted.get(i);
            Pill p = new Pill();
            p.worn = worn.codes.contains(item.code);
            p.text = Cosmetics.label(item.code) + ", " + timeLeft(item.expiresAt);
            p.w = fm.stringWidth(p.text) + 28;
            if (x + p.w > PANEL_RIGHT) {
                x = PANEL_LEFT;
                y += PILL_H + PILL_GAP;
            }
            p.x = x;
            p.y = y;
            x += p.w + PILL_GAP;
            pills.add(p);
        }
        if (sorted.size() > shown) {
            Pill more = new Pill();
            more.text = "+" + (sorted.size() - shown) + " more";
            more.w = fm.stringWidth(more.text) + 28;
            if (x + more.w > PANEL_RIGHT) {
                x = PANEL_LEFT;
                y += PILL_H + PILL_GAP;
            }
            more.x = x;
            more.y = y;
            pills.add(more);
        }
        return pills;
    }

    private static int panelHeight(List<Pill> pills) {
        if (pills.isEmpty()) return 0;
        int lastRowY = 0;
        for (Pill p : pills) lastRowY = Math.max(lastRowY, p.y);
        return 40 + lastRowY;
    }

    private static void drawInventory(Graphics2D g, List<Pill> pills, Cosmetics.Loadout worn, int x, int y) {
        Ui.divider(g, x, 488, HEIGHT - 42);
        if (pills.isEmpty()) {
            g.setFont(new Font(Font.DIALOG, Font.ITALIC, 13));
            g.setColor(Ui.muted());
            g.drawString("You own nothing yet. Try !shop, then !equip.", x, HEIGHT - 22);
            return;
        }
        g.setFont(Ui.bold(13));
        g.setColor(Ui.text());
        g.drawString("Inventory", x, y + 4);
        g.setFont(Ui.plain(12));
        g.setColor(Ui.muted());
        g.drawString("pressed = wearing now", x + 82, y + 4);
        int top = y + 18;
        for (Pill p : pills) {
            Ui.button(g, p.x, top + p.y, p.w, PILL_H, p.text, Ui.bold(14), p.worn);
            if (p.special) Ui.ring(g, p.x, top + p.y, p.w, PILL_H);
        }
    }

    private static String fit(Graphics2D g, String text, int maxWidth) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) return text;
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t + "…") > maxWidth) t = t.substring(0, t.length() - 1);
        return t + "…";
    }
}

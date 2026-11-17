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
    /** Height of the card with no inventory panel. */
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
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
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
     * Same card as {@link #render}, as a looping GIF: the mascot bobs, stars twinkle around it and a
     * glint sweeps across the card.
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
        BufferedImage mascot = Sticker.render(loadout.codes, 400, seed);
        java.util.Random random = new java.util.Random(seed);
        double[][] stars = new double[9][];
        for (int i = 0; i < stars.length; i++) {
            // x, y, size, phase
            stars[i] = new double[]{470 + random.nextInt(400), 30 + random.nextInt(340), 7 + random.nextInt(9),
                    random.nextDouble()};
        }

        javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.prepareWriteSequence(null);
            for (int i = 0; i < FRAMES; i++) {
                double t = i / (double) FRAMES;
                BufferedImage img = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    drawBase(g, loadout, height);
                    int bob = (int) Math.round(5 * Math.sin(2 * Math.PI * t));
                    g.drawImage(mascot, 510, 22 + bob, 390, 390, null);
                    drawFront(g, s, loadout, pills, height);
                    for (double[] star : stars) {
                        double glow = Math.max(0, Math.sin(2 * Math.PI * (t + star[3])));
                        if (glow > 0.15) twinkle(g, star[0], star[1], star[2] * (0.5 + glow), (int) (60 + 195 * glow));
                    }
                    glint(g, t, height);
                } finally {
                    g.dispose();
                }
                writer.writeToSequence(new javax.imageio.IIOImage(img, null, frameMetadata(writer, img, i == 0)), null);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
        byte[] bytes = out.toByteArray();
        return bytes.length > MAX_GIF_BYTES ? null : bytes;
    }

    private static javax.imageio.metadata.IIOMetadata frameMetadata(javax.imageio.ImageWriter writer, BufferedImage img,
                                                                    boolean first) throws IOException {
        javax.imageio.metadata.IIOMetadata md = writer.getDefaultImageMetadata(
                javax.imageio.ImageTypeSpecifier.createFromRenderedImage(img), null);
        String format = "javax_imageio_gif_image_1.0";
        javax.imageio.metadata.IIOMetadataNode root = (javax.imageio.metadata.IIOMetadataNode) md.getAsTree(format);
        javax.imageio.metadata.IIOMetadataNode gce = child(root, "GraphicControlExtension");
        gce.setAttribute("disposalMethod", "restoreToBackgroundColor");
        gce.setAttribute("delayTime", Integer.toString(FRAME_MS / 10));
        if (first) {
            javax.imageio.metadata.IIOMetadataNode apps = child(root, "ApplicationExtensions");
            javax.imageio.metadata.IIOMetadataNode app = new javax.imageio.metadata.IIOMetadataNode("ApplicationExtension");
            app.setAttribute("applicationID", "NETSCAPE");
            app.setAttribute("authenticationCode", "2.0");
            app.setUserObject(new byte[]{1, 0, 0}); // loop forever
            apps.appendChild(app);
        }
        md.setFromTree(format, root);
        return md;
    }

    private static javax.imageio.metadata.IIOMetadataNode child(javax.imageio.metadata.IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(name)) return (javax.imageio.metadata.IIOMetadataNode) root.item(i);
        }
        javax.imageio.metadata.IIOMetadataNode node = new javax.imageio.metadata.IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }

    /** A four-point star in gold, for the twinkle. */
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
        g.setColor(new Color(255, 244, 190, Math.min(255, alpha)));
        g.fill(star);
    }

    /** A soft diagonal shine that crosses the card once per loop. */
    private static void glint(Graphics2D g, double t, int height) {
        Shape card = new RoundRectangle2D.Float(8, 8, WIDTH - 16, height - 16, 36, 36);
        java.awt.Shape oldClip = g.getClip();
        g.setClip(card);
        double x = -260 + t * (WIDTH + 520);
        Color clear = new Color(255, 255, 255, 0);
        Color shine = new Color(255, 255, 255, 46);
        g.setPaint(new java.awt.LinearGradientPaint((float) x, 0, (float) (x + 200), 120,
                new float[]{0f, 0.5f, 1f}, new Color[]{clear, shine, clear}));
        g.fillRect(0, 0, WIDTH, height);
        g.setClip(oldClip);
    }

    // ----------------------------------------------------------------- draw

    private static void draw(Graphics2D g, Stats s, Cosmetics.Loadout l, List<Pill> pills, int height, long seed)
            throws IOException {
        drawBase(g, l, height);
        // mascot, wearing the loadout
        BufferedImage mascot = Sticker.render(l.codes, 400, seed);
        g.drawImage(mascot, 510, 22, 390, 390, null);
        drawFront(g, s, l, pills, height);
    }

    /** The card background, border and glow: everything behind the mascot. */
    private static void drawBase(Graphics2D g, Cosmetics.Loadout l, int height) {
        boolean bundle = l.has("bundle-weekend");
        boolean glow = l.has("flair-day");
        Color top = bundle ? new Color(0xFF2A2A) : new Color(0xA30000);
        Color bottom = bundle ? new Color(0x8A0000) : new Color(0x2E0000);
        Shape card = new RoundRectangle2D.Float(8, 8, WIDTH - 16, height - 16, 36, 36);

        g.setPaint(new GradientPaint(0, 0, top, WIDTH, height, bottom));
        g.fill(card);
        g.setClip(card);
        g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 46), 0, height * 0.4f, new Color(255, 255, 255, 0)));
        g.fillRect(0, 0, WIDTH, height);
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
        g.draw(new RoundRectangle2D.Float(2, 2, WIDTH - 4, height - 4, 42, 42));
        if (bundle) {
            g.setColor(GOLD);
            g.setStroke(new BasicStroke(2.5f));
            g.draw(new RoundRectangle2D.Float(24, 24, WIDTH - 48, height - 48, 26, 26));
        }

    }

    /** Everything in front of the mascot: avatar, name, stats, inventory. */
    private static void drawFront(Graphics2D g, Stats s, Cosmetics.Loadout l, List<Pill> pills, int height) {
        drawAvatar(g, s, 44, 44, 128);
        drawNameBlock(g, s, l, 196);
        drawStats(g, s, 44, 226);
        drawInventory(g, pills, l, 44, HEIGHT - 24);

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(255, 221, 0, 170));
        String tag = "GRYPHCOIN";
        g.drawString(tag, WIDTH - 40 - g.getFontMetrics().stringWidth(tag), height - 30);
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

        int y = 106;
        if (l.alumni) {
            ribbon(g, x, y, "\u2605 ALUMNI" + (l.alumniYear > 0 ? "  \u00B7  Class of " + l.alumniYear : ""));
            y += 40;
        }
        if (l.has("bundle-weekend") || l.has("title-week")) {
            ribbon(g, x, y, l.has("bundle-weekend") ? "the Weekender" : "the Scholar");
            y += 40;
        }
        if (l.has("banner-club")) {
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            String team = fit(g, l.team != null ? l.team : "Team", 260) + " banner";
            g.setColor(INK);
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            java.awt.font.GlyphVector tv = g.getFont().createGlyphVector(g.getFontRenderContext(), team);
            Shape to = tv.getOutline(x + 2, y + 20);
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
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
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
            p.text = (p.worn ? "\u2713 " : "") + Cosmetics.label(item.code) + "  \u00B7  " + timeLeft(item.expiresAt);
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
        return 40 + lastRowY + PILL_H + 22;
    }

    private static void drawInventory(Graphics2D g, List<Pill> pills, Cosmetics.Loadout worn, int x, int y) {
        if (pills.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.ITALIC, 13));
            g.setColor(new Color(255, 255, 255, 150));
            g.drawString("You own nothing yet. Try !shop, then !equip.", x, HEIGHT - 48);
            return;
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(255, 221, 0, 230));
        g.drawString("INVENTORY", x, y + 4);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(255, 255, 255, 150));
        g.drawString("\u2713 = wearing now", x + 96, y + 4);
        int top = y + 18;
        for (Pill p : pills) {
            Shape shape = new RoundRectangle2D.Float(p.x, top + p.y, p.w, PILL_H, PILL_H, PILL_H);
            g.setColor(p.worn ? GOLD : p.special ? new Color(0xB30000) : new Color(0, 0, 0, 130));
            g.fill(shape);
            g.setColor(p.special ? GOLD : INK);
            g.setStroke(new BasicStroke(p.special ? 2.5f : 3f));
            g.draw(shape);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(p.worn ? INK : Color.WHITE);
            g.drawString(p.text, p.x + 14, top + p.y + 20);
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

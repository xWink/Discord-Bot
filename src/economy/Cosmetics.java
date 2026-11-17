package economy;

import database.Database;

import java.awt.Color;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Equipping and rendering cosmetics. One item per slot (the item's category).
 * Cosmetics are display only: they never change payouts or ranked scores.
 */
public final class Cosmetics {

    private Cosmetics() { }

    /** How an item looks when equipped. */
    private static final class Look {
        final String badge;     // shown on the profile
        final String title;     // shown after the player's name
        final Color color;      // profile border
        final String effect;    // appended to wins
        final String icon;      // small mark shown next to the name
        final String banner;    // emoji row across the top of a win card
        final String heading;   // win card heading

        Look(String badge, String title, Color color, String effect) {
            this(badge, title, color, effect, "", null, null);
        }

        Look(String badge, String title, Color color, String effect, String icon, String banner, String heading) {
            this.badge = badge;
            this.title = title;
            this.color = color;
            this.effect = effect;
            this.icon = icon;
            this.banner = banner;
            this.heading = heading;
        }
    }

    private static final Map<String, Look> LOOKS = new HashMap<>();

    static {
        LOOKS.put("flair-day", new Look("\u2728 Daily flair", null, new Color(0xF5C542), null, "\u2728", null, null));
        LOOKS.put("victory-fx", new Look("\uD83C\uDF89 Confetti effect", null, null, "\uD83C\uDF89\uD83C\uDF8A\u2728", "",
                "\uD83C\uDF89\uD83C\uDF8A\u2728\uD83C\uDF8A\uD83C\uDF89\uD83C\uDF8A\u2728\uD83C\uDF8A\uD83C\uDF89", "Victory!"));
        LOOKS.put("fx-fireworks", new Look("\uD83C\uDF86 Fireworks effect", null, null, "\uD83C\uDF86\uD83C\uDF87", "",
                "\uD83C\uDF86\uD83C\uDF87\uD83C\uDF86\uD83C\uDF87\uD83C\uDF86\uD83C\uDF87\uD83C\uDF86", "Fireworks!"));
        LOOKS.put("fx-trophy", new Look("\uD83C\uDFC6 Golden trophy effect", null, null, "\uD83C\uDFC6", "",
                "\uD83C\uDFC6\uD83E\uDD47\uD83C\uDFC6\uD83E\uDD47\uD83C\uDFC6\uD83E\uDD47\uD83C\uDFC6", "Champion!"));
        LOOKS.put("fx-stars", new Look("\u2B50 Starfall effect", null, null, "\u2B50\u2728", "",
                "\u2728\u2B50\uD83C\uDF1F\u2B50\u2728\u2B50\uD83C\uDF1F\u2B50\u2728", "Starfall!"));
        LOOKS.put("bundle-weekend", new Look("\uD83C\uDF1F Weekend bundle (banner, frame, title)", "the Weekender",
                new Color(0xE0457B), null, "\uD83C\uDF1F", null, null));
        LOOKS.put("title-week", new Look("\uD83D\uDCDC Weekly title", "the Scholar", null, null));
        LOOKS.put("badge-set", new Look("\uD83C\uDFC5 Badge set", null, null, null, "\uD83C\uDFC5", null, null));
        LOOKS.put("badge-month", new Look("\uD83C\uDF96 Monthly collectible", null, null, null, "\uD83C\uDF96", null, null));
    }

    /** What a player's equipped items add up to. */
    public static final class Extras {
        public Color color;
        public String title = "";
        public String effect = "";
        public String icons = "";
        public String banner;
        public String heading;
        public Color effectColor;
        public String effectCode;
        public final StringBuilder badges = new StringBuilder();
    }

    private static String slotOf(java.sql.Connection c, String code) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT category FROM shop_items WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "badge";
            }
        }
    }

    /**
     * Equips an owned, unexpired item into its slot.
     *
     * @return the slot name, or null if the player does not own it
     */
    public static String equip(long user, String code) throws SQLException {
        return Database.inTransaction(c -> {
            long now = System.currentTimeMillis();
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM inventory WHERE user = ? AND item_code = ? "
                    + "AND (expires_at = 0 OR expires_at > ?) LIMIT 1")) {
                ps.setLong(1, user);
                ps.setString(2, code);
                ps.setLong(3, now);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                }
            }
            String slot = slotOf(c, code);
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO equipped (user, slot, item_code) VALUES "
                    + "(?, ?, ?) ON DUPLICATE KEY UPDATE item_code = VALUES(item_code)")) {
                ps.setLong(1, user);
                ps.setString(2, slot);
                ps.setString(3, code);
                ps.executeUpdate();
            }
            return slot;
        });
    }

    public static void unequipAll(long user) throws SQLException {
        Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM equipped WHERE user = ?")) {
                ps.setLong(1, user);
                ps.executeUpdate();
            }
            return null;
        });
    }

    /**
     * @return the player's equipped items that are still owned and unexpired
     */
    public static Extras extras(long user) {
        Extras ex = new Extras();
        try {
            Database.inTransaction(c -> {
                try (PreparedStatement ps = c.prepareStatement("SELECT e.item_code FROM equipped e WHERE e.user = ? "
                        + "AND EXISTS (SELECT 1 FROM inventory i WHERE i.user = e.user AND i.item_code = e.item_code "
                        + "AND (i.expires_at = 0 OR i.expires_at > ?)) ORDER BY e.slot")) {
                    ps.setLong(1, user);
                    ps.setLong(2, System.currentTimeMillis());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) apply(ex, rs.getString(1));
                    }
                }
                if (AlumniService.isAlumniIn(c, user)) {
                    ex.icons += "\uD83C\uDF93";
                    ex.badges.append("\uD83C\uDF93 Alumni\n");
                }
                if (ex.badges.indexOf("Team banner") >= 0) {
                    String team = BannerService.activeTeam(c, user);
                    if (team != null) {
                        int at = ex.badges.indexOf("Team banner");
                        ex.badges.replace(at, at + "Team banner".length(), team + " banner");
                        ex.color = new Color(0x1FA3A3);
                    }
                }
                return null;
            });
        } catch (SQLException e) {
            e.printStackTrace(); // cosmetics are optional; show the plain version
        }
        return ex;
    }

    /** A readable name for an item code, for the profile card and messages. */
    public static String label(String code) {
        switch (code) {
            case "flair-day": return "Daily flair";
            case "victory-fx": return "Confetti";
            case "fx-fireworks": return "Fireworks";
            case "fx-trophy": return "Golden trophy";
            case "fx-stars": return "Starfall";
            case "bundle-weekend": return "Weekend bundle";
            case "title-week": return "Scholar title";
            case "badge-set": return "Badge set";
            case "badge-month": return "Monthly badge";
            case "banner-club": return "Team banner";
            case "event-pass": return "Event pass";
            case "alumni": return "Alumni";
            case "gift-wrap": return "Gift wrap";
            default:
                if (code.startsWith("season-")) return "Season winner";
                if (code.startsWith("gauntlet-")) return "Gauntlet clear";
                return code;
        }
    }

    /** One owned item and when it runs out (0 means permanent). */
    public static final class OwnedItem {
        public final String code;
        public final long expiresAt;

        public OwnedItem(String code, long expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    /**
     * @return every item the player owns that has not expired, one entry per item with its latest expiry
     */
    public static java.util.List<OwnedItem> owned(long user) {
        java.util.List<OwnedItem> list = new java.util.ArrayList<>();
        try {
            Database.inTransaction(c -> {
                try (PreparedStatement ps = c.prepareStatement("SELECT item_code, "
                        + "CASE WHEN MIN(expires_at) = 0 THEN 0 ELSE MAX(expires_at) END FROM inventory "
                        + "WHERE user = ? GROUP BY item_code HAVING MIN(expires_at) = 0 OR MAX(expires_at) > ?")) {
                    ps.setLong(1, user);
                    ps.setLong(2, System.currentTimeMillis());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) list.add(new OwnedItem(rs.getString(1), rs.getLong(2)));
                    }
                }
                return null;
            });
        } catch (SQLException e) {
            e.printStackTrace(); // the card just shows no inventory
        }
        return list;
    }

    /** The codes a player is currently wearing (owned and unexpired), plus their team name if any. */
    public static final class Loadout {
        public final java.util.List<String> codes = new java.util.ArrayList<>();
        public String team;
        /** The class year a mod chose, or 0 if none. Alumni also appears in {@code codes}. */
        public int alumniYear;
        /** True for alumni, whether or not a class year was given. */
        public boolean alumni;

        public boolean has(String prefix) {
            for (String c : codes) if (c.startsWith(prefix)) return true;
            return false;
        }
    }

    public static Loadout loadout(long user) {
        Loadout l = new Loadout();
        try {
            Database.inTransaction(c -> {
                try (PreparedStatement ps = c.prepareStatement("SELECT e.item_code FROM equipped e WHERE e.user = ? "
                        + "AND EXISTS (SELECT 1 FROM inventory i WHERE i.user = e.user AND i.item_code = e.item_code "
                        + "AND (i.expires_at = 0 OR i.expires_at > ?)) ORDER BY e.slot")) {
                    ps.setLong(1, user);
                    ps.setLong(2, System.currentTimeMillis());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) l.codes.add(rs.getString(1));
                    }
                }
                if (l.has("banner-club")) l.team = BannerService.activeTeam(c, user);
                l.alumni = AlumniService.isAlumniIn(c, user);
                l.alumniYear = l.alumni ? AlumniService.classYearIn(c, user) : 0;
                if (l.alumni) l.codes.add("alumni");
                return null;
            });
        } catch (SQLException e) {
            e.printStackTrace(); // the card falls back to the default look
        }
        return l;
    }

    static void apply(Extras ex, String code) {
        Look look = LOOKS.get(code);
        if (look == null) {
            if (code.startsWith("season-")) { ex.badges.append("🏆 Season winner\n"); ex.icons += "🏆"; }
            else if (code.startsWith("gauntlet-")) { ex.badges.append("🧩 Gauntlet clear\n"); ex.icons += "🧩"; }
            else if (code.startsWith("banner-club")) { ex.badges.append("🚩 Team banner\n"); ex.icons += "🚩"; }
            return;
        }
        ex.badges.append(look.badge).append('\n');
        if (look.title != null) ex.title = " " + look.title;
        if (look.color != null) ex.color = look.color;
        if (look.effect != null) ex.effect = " " + look.effect;
        ex.icons += look.icon;
        if (look.banner != null) {
            ex.banner = look.banner;
            ex.heading = look.heading;
            ex.effectColor = new Color(0xFFDD00);
            ex.effectCode = code;
        }
    }

    /** The name followed by an equipped title and badge marks, if any. */
    public static String nameWithTitle(long user, String name) {
        Extras ex = extras(user);
        return name + ex.title + (ex.icons.isEmpty() ? "" : " " + ex.icons);
    }

    /** The top of a win message for someone with an effect equipped, or an empty string. */
    public static String winHeader(long user) {
        Extras ex = extras(user);
        return ex.banner == null ? "" : "\u2726 **" + ex.heading + "** \u2726\n";
    }

    /** Title and marks to put after a mention, e.g. " the Scholar \uD83C\uDFC5". Empty if none. */
    public static String tag(long user) {
        Extras ex = extras(user);
        return ex.title + (ex.icons.isEmpty() ? "" : " " + ex.icons);
    }

    private static final java.util.Map<String, byte[]> WIN_ART = new java.util.concurrent.ConcurrentHashMap<>();

    /** The mascot wearing one effect, cached because it is the same for everyone who owns that effect. */
    private static byte[] winArt(String effectCode) throws java.io.IOException {
        byte[] cached = WIN_ART.get(effectCode);
        if (cached != null) return cached;
        java.awt.image.BufferedImage img = command.util.art.Sticker.render(
                java.util.Collections.singletonList(effectCode), 256, 7);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", out);
        WIN_ART.put(effectCode, out.toByteArray());
        return out.toByteArray();
    }

    /**
     * Sends a result. A win by someone with a victory effect equipped is shown as a card with the mascot
     * celebrating in that effect's style; everything else is plain text.
     */
    public static void reply(net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel, long user,
                             String text, boolean won) {
        Extras ex = won ? extras(user) : null;
        if (ex == null || ex.effectCode == null) {
            channel.sendMessage(text).queue();
            return;
        }
        try {
            net.dv8tion.jda.api.EmbedBuilder eb = new net.dv8tion.jda.api.EmbedBuilder();
            eb.setColor(ex.effectColor);
            eb.setTitle(ex.heading);
            eb.setDescription(text);
            eb.setThumbnail("attachment://win.png");
            channel.sendFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(winArt(ex.effectCode), "win.png"))
                    .setEmbeds(eb.build()).queue();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            channel.sendMessage(text).queue(); // never lose the result because the picture failed
        }
    }

}

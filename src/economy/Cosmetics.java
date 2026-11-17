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

        Look(String badge, String title, Color color, String effect) {
            this.badge = badge;
            this.title = title;
            this.color = color;
            this.effect = effect;
        }
    }

    private static final Map<String, Look> LOOKS = new HashMap<>();

    static {
        LOOKS.put("flair-day", new Look("✨ Daily flair", null, new Color(0xF5C542), null));
        LOOKS.put("victory-fx", new Look("🎉 Victory effect", null, null, "🎉🎊✨"));
        LOOKS.put("bundle-weekend", new Look("🌟 Weekend bundle (banner, frame, title)", "the Weekender",
                new Color(0xE0457B), null));
        LOOKS.put("title-week", new Look("📜 Weekly title", "the Scholar", null, null));
        LOOKS.put("badge-set", new Look("🏅 Badge set", null, null, null));
        LOOKS.put("badge-month", new Look("🎖 Monthly collectible", null, null, null));
    }

    /** What a player's equipped items add up to. */
    public static final class Extras {
        public Color color;
        public String title = "";
        public String effect = "";
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

    static void apply(Extras ex, String code) {
        Look look = LOOKS.get(code);
        if (look == null) {
            if (code.startsWith("season-")) ex.badges.append("🏆 Season winner\n");
            else if (code.startsWith("gauntlet-")) ex.badges.append("🧩 Gauntlet clear\n");
            else if (code.startsWith("banner-club")) ex.badges.append("🚩 Team banner\n");
            return;
        }
        ex.badges.append(look.badge).append('\n');
        if (look.title != null) ex.title = " " + look.title;
        if (look.color != null) ex.color = look.color;
        if (look.effect != null) ex.effect = " " + look.effect;
    }

    /** The name followed by an equipped title, if any. */
    public static String nameWithTitle(long user, String name) {
        return name + extras(user).title;
    }
}

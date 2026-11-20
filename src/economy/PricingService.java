package economy;

import arcade.PunchCard;
import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Rotates the featured shelf and moves featured prices once per week. The
 * entry shelf, the colour rental, community goals and installment totals are
 * never repriced.
 */
public final class PricingService {

    public static final int FEATURED_COUNT = 3;
    private static final String SETTING = "shop_rotation_week";

    private PricingService() { }

    /**
     * Rotates if this week has not been rotated yet. Safe to call often.
     *
     * @return true if a rotation ran
     */
    public static boolean rotateIfDue() throws SQLException {
        int week = PunchCard.currentWeek();
        return Database.inTransaction(c -> {
            long last = -1;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT value FROM economy_settings WHERE name = ? FOR UPDATE")) {
                ps.setString(1, SETTING);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) last = rs.getLong(1);
                }
            }
            if (last == week) return false;

            // Last week's demand on the items that were featured.
            long since = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
            List<Object[]> featured = new ArrayList<>(); // code, base, price, buyers
            int sample = 0;
            try (PreparedStatement ps = c.prepareStatement("SELECT s.code, s.base_price, s.price, "
                    + "(SELECT COUNT(DISTINCT i.user) FROM inventory i WHERE i.item_code = s.code "
                    + "AND i.created_at >= ?) FROM shop_items s WHERE s.featured = 1")) {
                ps.setLong(1, since);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        featured.add(new Object[]{rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)});
                        sample += rs.getInt(4);
                    }
                }
            }
            for (Object[] f : featured) {
                int base = (Integer) f[1];
                int old = (Integer) f[2];
                int price = last < 0 ? old : PriceMath.next(base, old, (Integer) f[3], sample);
                try (PreparedStatement ps = c.prepareStatement("UPDATE shop_items SET price = ?, "
                        + "prev_price = IF(? <> price, price, prev_price), price_updated_at = ? WHERE code = ?")) {
                    ps.setInt(1, price);
                    ps.setInt(2, price);
                    ps.setLong(3, System.currentTimeMillis());
                    ps.setString(4, (String) f[0]);
                    ps.executeUpdate();
                }
            }

            // New featured shelf: a rotating window over the pool, by week.
            List<String> pool = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT code FROM shop_items WHERE rotating = 1 AND active = 1 ORDER BY code");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) pool.add(rs.getString(1));
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE shop_items SET featured = 0 WHERE rotating = 1")) {
                ps.executeUpdate();
            }
            for (int i = 0; i < Math.min(FEATURED_COUNT, pool.size()); i++) {
                try (PreparedStatement ps = c.prepareStatement("UPDATE shop_items SET featured = 1 WHERE code = ?")) {
                    ps.setString(1, pool.get(Math.floorMod(week + i, pool.size())));
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO economy_settings (name, value) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE value = VALUES(value)")) {
                ps.setString(1, SETTING);
                ps.setLong(2, week);
                ps.executeUpdate();
            }
            return true;
        });
    }
}

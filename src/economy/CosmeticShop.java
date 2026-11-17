package economy;

import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cosmetic items that need no Discord grant, so payment and delivery happen in
 * one transaction. Cosmetics never affect game payouts or ranked scores.
 */
public final class CosmeticShop {

    private static final CosmeticShop INSTANCE = new CosmeticShop();

    private CosmeticShop() { }

    public static CosmeticShop getInstance() {
        return INSTANCE;
    }

    /** A catalog entry. */
    public static final class Item {
        public final String code;
        public final String name;
        public final String category;
        public final int price;
        public final long durationMs;
        public final boolean featured;

        Item(String code, String name, String category, int price, long durationMs, boolean featured) {
            this.code = code;
            this.name = name;
            this.category = category;
            this.price = price;
            this.durationMs = durationMs;
            this.featured = featured;
        }
    }

    public List<Item> catalog() throws SQLException {
        return Database.inTransaction(c -> {
            List<Item> items = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT code, name, category, price, duration_ms, "
                    + "featured FROM shop_items WHERE active = 1 ORDER BY featured DESC, price")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(new Item(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4),
                                rs.getLong(5), rs.getInt(6) != 0));
                    }
                }
            }
            return items;
        });
    }

    /**
     * Buys an item. Permanent items can only be owned once; timed items
     * extend an unexpired copy.
     *
     * @return the item, or null if the code is unknown or the permanent item is already owned
     */
    public Item buy(long user, String code, String purchaseId) throws SQLException, InsufficientFundsException {
        try {
            return Database.inTransaction(c -> {
                Item item = null;
                try (PreparedStatement ps = c.prepareStatement("SELECT code, name, category, price, duration_ms, "
                        + "featured FROM shop_items WHERE code = ? AND active = 1")) {
                    ps.setString(1, code);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            item = new Item(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4),
                                    rs.getLong(5), rs.getInt(6) != 0);
                        }
                    }
                }
                if (item == null) return null;
                EconomyService.lockWallet(c, user); // serialize this player's purchases
                long now = System.currentTimeMillis();
                long base = now;
                try (PreparedStatement ps = c.prepareStatement("SELECT MAX(expires_at) FROM inventory "
                        + "WHERE user = ? AND item_code = ?")) {
                    ps.setLong(1, user);
                    ps.setString(2, code);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        long latest = rs.getLong(1);
                        boolean owned = rs.getObject(1) != null;
                        if (item.durationMs == 0 && owned) return null;
                        if (latest > now) base = latest;
                    }
                }
                EconomyService.debitIn(c, user, item.price, "shop:item", code, purchaseId);
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO inventory (user, item_code, purchase_id, "
                        + "expires_at, created_at) VALUES (?, ?, ?, ?, ?)")) {
                    ps.setLong(1, user);
                    ps.setString(2, code);
                    ps.setString(3, purchaseId);
                    ps.setLong(4, item.durationMs == 0 ? 0 : base + item.durationMs);
                    ps.setLong(5, now);
                    ps.executeUpdate();
                }
                return item;
            });
        } catch (EconomyService.InsufficientFundsRuntime e) {
            throw new InsufficientFundsException(0, 0);
        }
    }

    /**
     * @return one line per owned, unexpired item
     */
    public List<String> owned(long user) throws SQLException {
        return Database.inTransaction(c -> {
            List<String> lines = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT s.name, MAX(i.expires_at) FROM inventory i "
                    + "JOIN shop_items s ON s.code = i.item_code WHERE i.user = ? GROUP BY s.name")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    long now = System.currentTimeMillis();
                    while (rs.next()) {
                        long exp = rs.getLong(2);
                        if (exp == 0) lines.add(rs.getString(1) + " (permanent)");
                        else if (exp > now) lines.add(rs.getString(1) + " (until <t:" + exp / 1000 + ":R>)");
                    }
                }
            }
            return lines;
        });
    }
}

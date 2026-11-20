package courses;

import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * A person's marks in a course: each item (assignment, midterm, final) has a weight in percent and, once marked, a mark in
 * percent. Works out the current standing and what is needed on the rest to reach a target. The numbers are the person's
 * own and are only ever shown to them.
 */
public final class Grades {

    public static final int MAX_ITEMS_PER_COURSE = 20;

    private Grades() { }

    /** One graded or ungraded piece of a course. */
    public static final class Item {
        public final String name;
        public final double weight;
        /** Percent, or null if it has not been marked yet. */
        public final Double mark;

        public Item(String name, double weight, Double mark) {
            this.name = name;
            this.weight = weight;
            this.mark = mark;
        }
    }

    /** Where a course stands. */
    public static final class Standing {
        /** Total weight of the items listed. */
        public final double listedWeight;
        /** Weight of the items that have a mark. */
        public final double gradedWeight;
        /** Percentage points of the final grade already earned. */
        public final double earned;
        /** The average of the marked items (null if none is marked). */
        public final Double average;
        /** Weight not yet marked, counting weight not listed at all: 100 minus the graded weight. */
        public final double remaining;
        /** The best final grade possible: full marks on everything left. */
        public final double best;
        /** The final grade if everything left were zero. */
        public final double worst;

        Standing(double listedWeight, double gradedWeight, double earned) {
            this.listedWeight = listedWeight;
            this.gradedWeight = gradedWeight;
            this.earned = earned;
            this.average = gradedWeight > 0 ? earned / gradedWeight * 100 : null;
            this.remaining = Math.max(0, 100 - gradedWeight);
            this.best = earned + remaining;
            this.worst = earned;
        }
    }

    public static Standing standing(List<Item> items) {
        double listed = 0;
        double graded = 0;
        double earned = 0;
        for (Item i : items) {
            listed += i.weight;
            if (i.mark != null) {
                graded += i.weight;
                earned += i.weight * i.mark / 100.0;
            }
        }
        return new Standing(listed, graded, earned);
    }

    /** What is needed on what is left. */
    public enum Outcome { SECURED, POSSIBLE, IMPOSSIBLE, NOTHING_LEFT }

    public static final class Need {
        public final Outcome outcome;
        /** The average needed on the remaining weight, in percent (meaningful for POSSIBLE). */
        public final double average;
        public final double remaining;

        Need(Outcome outcome, double average, double remaining) {
            this.outcome = outcome;
            this.average = average;
            this.remaining = remaining;
        }
    }

    /** @param target the final grade wanted, in percent */
    public static Need need(List<Item> items, double target) {
        Standing s = standing(items);
        if (s.earned >= target) return new Need(Outcome.SECURED, 0, s.remaining);
        if (s.remaining <= 0.0001) return new Need(Outcome.NOTHING_LEFT, 0, 0);
        double needed = (target - s.earned) / s.remaining * 100;
        return needed > 100.0001 ? new Need(Outcome.IMPOSSIBLE, needed, s.remaining) : new Need(Outcome.POSSIBLE, needed, s.remaining);
    }

    // ------------------------------------------------------------------ storage

    /** @return true if the item is new, false if it replaced one with the same name; -1 style limit shown as an exception */
    public static boolean save(long user, String code, Item item) throws SQLException {
        return Database.inTransaction(c -> {
            boolean exists;
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM grade_items WHERE user = ? AND code = ? AND name = ? FOR UPDATE")) {
                ps.setLong(1, user);
                ps.setString(2, code);
                ps.setString(3, item.name);
                try (ResultSet rs = ps.executeQuery()) {
                    exists = rs.next();
                }
            }
            if (!exists) {
                try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM grade_items WHERE user = ? AND code = ?")) {
                    ps.setLong(1, user);
                    ps.setString(2, code);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) >= MAX_ITEMS_PER_COURSE) throw new IllegalStateException("too many items");
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO grade_items (user, code, name, weight, mark, created) "
                    + "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE weight = VALUES(weight), mark = VALUES(mark)")) {
                ps.setLong(1, user);
                ps.setString(2, code);
                ps.setString(3, item.name);
                ps.setDouble(4, item.weight);
                if (item.mark == null) ps.setNull(5, java.sql.Types.DECIMAL);
                else ps.setDouble(5, item.mark);
                ps.setLong(6, System.currentTimeMillis());
                ps.executeUpdate();
            }
            return !exists;
        });
    }

    public static List<Item> items(long user, String code) throws SQLException {
        return Database.inTransaction(c -> {
            List<Item> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT name, weight, mark FROM grade_items WHERE user = ? AND code = ? ORDER BY id")) {
                ps.setLong(1, user);
                ps.setString(2, code);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString(1);
                        double weight = rs.getDouble(2);
                        double mark = rs.getDouble(3);
                        boolean unmarked = rs.wasNull(); // must be asked straight after reading the mark
                        out.add(new Item(name, weight, unmarked ? null : mark));
                    }
                }
            }
            return out;
        });
    }

    /** @return the courses the person has any items for */
    public static java.util.Set<String> courses(long user) throws SQLException {
        return Database.inTransaction(c -> {
            java.util.Set<String> out = new TreeSet<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT DISTINCT code FROM grade_items WHERE user = ?")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(rs.getString(1));
                }
            }
            return out;
        });
    }

    public static boolean remove(long user, String code, String name) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM grade_items WHERE user = ? AND code = ? AND name = ?")) {
                ps.setLong(1, user);
                ps.setString(2, code);
                ps.setString(3, name);
                return ps.executeUpdate() > 0;
            }
        });
    }

    /** @return how many items were removed */
    public static int clear(long user, String code) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM grade_items WHERE user = ? AND code = ?")) {
                ps.setLong(1, user);
                ps.setString(2, code);
                return ps.executeUpdate();
            }
        });
    }
}

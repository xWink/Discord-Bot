package courses;

import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Student ratings of courses: difficulty and workload from 1 to 5, and an optional comment. One rating per person per
 * course (rating again replaces it). Ratings are anonymous on screen. A comment reported by {@link #REPORTS_TO_HIDE}
 * different people is hidden, and moderators can hide any. A moderator can also take a rating out of the averages
 * altogether ({@link #exclude}); that stays even if the person rates again.
 */
public final class Ratings {

    public static final int MAX_COMMENT = 300;
    public static final int REPORTS_TO_HIDE = 3;
    /** Averages are only shown once this many people have rated, so one person's view is not presented as a trend. */
    public static final int SHOW_AVERAGE_FROM = 3;

    private Ratings() { }

    /** What students think of a course. */
    public static final class Summary {
        public final int count;
        public final double difficulty;
        public final double workload;
        /** How many people gave each score, index 0 for a 1 up to index 4 for a 5. */
        public final int[] difficultyCounts = new int[5];
        public final int[] workloadCounts = new int[5];

        Summary(int count, double difficulty, double workload) {
            this.count = count;
            this.difficulty = difficulty;
            this.workload = workload;
        }

        public boolean enoughToShow() {
            return count >= SHOW_AVERAGE_FROM;
        }
    }

    /** One comment, without who wrote it. */
    public static final class Review {
        public final long id;
        public final int difficulty;
        public final int workload;
        public final String comment;
        public final long created;

        Review(long id, int difficulty, int workload, String comment, long created) {
            this.id = id;
            this.difficulty = difficulty;
            this.workload = workload;
            this.comment = comment;
            this.created = created;
        }
    }

    /** @return true if this was the person's first rating of the course, false if it replaced an earlier one */
    public static boolean rate(long user, String code, int difficulty, int workload, String comment) throws SQLException {
        String text = comment == null || comment.trim().isEmpty() ? null
                : comment.trim().substring(0, Math.min(MAX_COMMENT, comment.trim().length()));
        return Database.inTransaction(c -> {
            boolean existed;
            try (PreparedStatement ps = c.prepareStatement("SELECT id FROM course_ratings WHERE user = ? AND code = ? FOR UPDATE")) {
                ps.setLong(1, user);
                ps.setString(2, code);
                try (ResultSet rs = ps.executeQuery()) {
                    existed = rs.next();
                }
            }
            if (existed) {
                // a changed rating starts clean: earlier reports were about the old words
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM course_rating_reports WHERE rating_id = "
                        + "(SELECT id FROM course_ratings WHERE user = ? AND code = ?)")) {
                    ps.setLong(1, user);
                    ps.setString(2, code);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE course_ratings SET difficulty = ?, workload = ?, comment = ?, "
                        + "created = ?, hidden = 0 WHERE user = ? AND code = ?")) {
                    ps.setInt(1, difficulty);
                    ps.setInt(2, workload);
                    ps.setString(3, text);
                    ps.setLong(4, System.currentTimeMillis());
                    ps.setLong(5, user);
                    ps.setString(6, code);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO course_ratings (user, code, difficulty, workload, "
                        + "comment, created) VALUES (?, ?, ?, ?, ?, ?)")) {
                    ps.setLong(1, user);
                    ps.setString(2, code);
                    ps.setInt(3, difficulty);
                    ps.setInt(4, workload);
                    ps.setString(5, text);
                    ps.setLong(6, System.currentTimeMillis());
                    ps.executeUpdate();
                }
            }
            return !existed;
        });
    }

    /** @return true if the person had a rating of the course and it is now gone */
    public static boolean remove(long user, String code) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM course_ratings WHERE user = ? AND code = ?")) {
                ps.setLong(1, user);
                ps.setString(2, code);
                return ps.executeUpdate() > 0;
            }
        });
    }

    /** @return the summary, or null if nobody has rated the course. Hidden comments still count towards the scores; excluded ratings do not. */
    public static Summary summary(String code) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT difficulty, workload FROM course_ratings WHERE code = ? AND excluded = 0")) {
                ps.setString(1, code);
                List<int[]> rows = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) rows.add(new int[]{rs.getInt(1), rs.getInt(2)});
                }
                if (rows.isEmpty()) return null;
                double d = 0;
                double w = 0;
                for (int[] r : rows) {
                    d += r[0];
                    w += r[1];
                }
                Summary s = new Summary(rows.size(), d / rows.size(), w / rows.size());
                for (int[] r : rows) {
                    s.difficultyCounts[r[0] - 1]++;
                    s.workloadCounts[r[1] - 1]++;
                }
                return s;
            }
        });
    }

    /** Like {@link #summary} but never throws: if the database cannot be reached there is simply no summary. */
    public static Summary summaryOrNull(String code) {
        try {
            return summary(code);
        } catch (Exception e) {
            return null;
        }
    }

    /** @return the newest visible comments (ratings without words are left out) */
    public static List<Review> comments(String code, int limit) throws SQLException {
        return Database.inTransaction(c -> {
            List<Review> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT id, difficulty, workload, comment, created FROM course_ratings "
                    + "WHERE code = ? AND hidden = 0 AND comment IS NOT NULL ORDER BY created DESC LIMIT ?")) {
                ps.setString(1, code);
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(new Review(rs.getLong(1), rs.getInt(2), rs.getInt(3), rs.getString(4), rs.getLong(5)));
                }
            }
            return out;
        });
    }

    /** What happened to a report. */
    public enum Reported { OK, HIDDEN, OWN, ALREADY, MISSING }

    public static Reported report(long reporter, long id) throws SQLException {
        return Database.inTransaction(c -> {
            long author;
            try (PreparedStatement ps = c.prepareStatement("SELECT user FROM course_ratings WHERE id = ? AND hidden = 0")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Reported.MISSING;
                    author = rs.getLong(1);
                }
            }
            if (author == reporter) return Reported.OWN;
            try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO course_rating_reports (rating_id, reporter) VALUES (?, ?)")) {
                ps.setLong(1, id);
                ps.setLong(2, reporter);
                if (ps.executeUpdate() == 0) return Reported.ALREADY;
            }
            int reports;
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM course_rating_reports WHERE rating_id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    reports = rs.getInt(1);
                }
            }
            if (reports >= REPORTS_TO_HIDE) {
                hideIn(c, id);
                return Reported.HIDDEN;
            }
            return Reported.OK;
        });
    }

    private static void hideIn(java.sql.Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE course_ratings SET hidden = 1 WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Moderators: hides the comment and takes the rating out of the averages, for a rating that is spam or abuse and
     * not just badly worded. @return false if there is no such rating
     */
    public static boolean exclude(long id) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE course_ratings SET hidden = 1, excluded = 1 WHERE id = ?")) {
                ps.setLong(1, id);
                return ps.executeUpdate() > 0;
            }
        });
    }

    /** Moderators: hides a comment at once; the scores still count. @return false if there is no such rating */
    public static boolean hide(long id) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE course_ratings SET hidden = 1 WHERE id = ?")) {
                ps.setLong(1, id);
                return ps.executeUpdate() > 0;
            }
        });
    }
}

package arcade;

import database.Database;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The persisted question pool. Rounds are always served from here, so the bank,
 * not an API call, is what prevents repeats and survives provider outages.
 */
public final class QuestionBank {

    private QuestionBank() { }

    /** Recently served questions are skipped for each player. */
    private static final int RECENT_WINDOW = 50;

    /**
     * Loads the bundled, pre-verified pool. Safe to call at every startup:
     * fingerprints make the insert idempotent.
     */
    public static void loadBundled() throws Exception {
        try (InputStream in = QuestionBank.class.getClassLoader().getResourceAsStream("arcade/bundled.json")) {
            if (in == null) return;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            JSONArray array = new JSONArray(new String(out.toByteArray(), StandardCharsets.UTF_8));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                List<String> choices = new ArrayList<>();
                for (int j = 0; j < o.getJSONArray("choices").length(); j++) {
                    choices.add(o.getJSONArray("choices").getString(j));
                }
                insert("bundled", o.getString("kind"), o.getString("category"), o.getString("difficulty"),
                        o.getString("prompt"), choices, o.getInt("answer"), o.optString("explanation", null),
                        "Project content", "Bundled with the bot");
            }
        }
    }

    /**
     * Adds a question unless an identical one (same prompt and choices) is
     * already stored.
     *
     * @return true if it was new
     */
    public static boolean insert(String provider, String kind, String category, String difficulty, String prompt,
                                 List<String> choices, int correctIndex, String explanation, String license,
                                 String attribution) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO question_bank (fingerprint, provider, "
                    + "kind, category, difficulty, prompt, choices, correct_index, explanation, license, "
                    + "attribution, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, fingerprint(prompt, choices));
                ps.setString(2, provider);
                ps.setString(3, kind);
                ps.setString(4, category);
                ps.setString(5, difficulty);
                ps.setString(6, prompt);
                ps.setString(7, new JSONArray(choices).toString());
                ps.setInt(8, correctIndex);
                ps.setString(9, explanation);
                ps.setString(10, license);
                ps.setString(11, attribution);
                ps.setLong(12, System.currentTimeMillis());
                return ps.executeUpdate() > 0;
            }
        });
    }

    /**
     * Picks a question of the given kind that this player has not seen
     * recently, falling back to any of that kind once they have seen them all.
     *
     * @param category optional category filter, or null
     * @return the question, or null if the bank has none of that kind
     */
    public static Question next(String kind, String category, long user) throws SQLException {
        return Database.inTransaction(c -> {
            Question q = pick(c, kind, category, user, true);
            return q != null ? q : pick(c, kind, category, user, false);
        });
    }

    /**
     * Like {@link #next} but matches the category by substring, case-insensitively.
     *
     * @return a question, or null if none match
     */
    public static Question nextMatching(String kind, String categoryText, long user) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT DISTINCT category FROM question_bank "
                    + "WHERE kind = ? AND LOWER(category) LIKE ? ORDER BY category LIMIT 1")) {
                ps.setString(1, kind);
                ps.setString(2, "%" + categoryText.toLowerCase().replace("%", "") + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    String exact = rs.getString(1);
                    Question q = pick(c, kind, exact, user, true);
                    return q != null ? q : pick(c, kind, exact, user, false);
                }
            }
        });
    }

    /** @return the categories available for a kind, alphabetical */
    public static List<String> categories(String kind) throws SQLException {
        return Database.inTransaction(c -> {
            List<String> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT DISTINCT category FROM question_bank WHERE kind = ? ORDER BY category LIMIT 40")) {
                ps.setString(1, kind);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(rs.getString(1));
                }
            }
            return list;
        });
    }

    /**
     * @return how many questions of this kind and category are stored
     */
    public static int stock(String kind, String category) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM question_bank WHERE kind = ?"
                    + (category != null ? " AND category = ?" : ""))) {
                ps.setString(1, kind);
                if (category != null) ps.setString(2, category);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
    }

    private static Question pick(Connection c, String kind, String category, long user, boolean excludeRecent)
            throws SQLException {
        String sql = "SELECT id, kind, category, difficulty, prompt, choices, correct_index, explanation, license, "
                + "attribution FROM question_bank WHERE kind = ?"
                + (category != null ? " AND category = ?" : "")
                + (excludeRecent ? " AND id NOT IN (SELECT question_id FROM (SELECT question_id FROM arcade_scores "
                + "WHERE user = ? ORDER BY id DESC LIMIT " + RECENT_WINDOW + ") recent)" : "")
                + " ORDER BY RAND() LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, kind);
            if (category != null) ps.setString(i++, category);
            if (excludeRecent) ps.setLong(i, user);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                JSONArray arr = new JSONArray(rs.getString(6));
                List<String> choices = new ArrayList<>();
                for (int j = 0; j < arr.length(); j++) choices.add(arr.getString(j));
                return new Question(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), choices, rs.getInt(7), rs.getString(8), rs.getString(9), rs.getString(10));
            }
        }
    }

    /**
     * Fingerprint of normalized prompt and sorted choices, so the same question
     * arriving in a later batch is recognised as a duplicate.
     */
    static String fingerprint(String prompt, List<String> choices) {
        List<String> sorted = new ArrayList<>();
        for (String choice : choices) sorted.add(normalize(choice));
        Collections.sort(sorted);
        return sha256(normalize(prompt) + "|" + String.join("|", sorted));
    }

    static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

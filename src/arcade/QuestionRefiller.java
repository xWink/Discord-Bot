package arcade;

import database.Database;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tops the bank up from Open Trivia DB (CC BY-SA 4.0, one request per IP every
 * five seconds, up to 50 questions per call). Refills run on one background
 * thread, so concurrent requests for the same category and difficulty share a
 * single refill and the provider's rate limit is respected.
 */
public final class QuestionRefiller {

    private static final String PROVIDER = "opentdb";
    private static final String ENDPOINT = "https://opentdb.com/api.php";
    private static final long MIN_REQUEST_GAP_MS = 5500;
    private static final long MIN_REFILL_INTERVAL_MS = 60L * 60 * 1000;
    private static final long MAX_BACKOFF_MS = 6L * 60 * 60 * 1000;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "question-refiller");
        t.setDaemon(true);
        return t;
    });
    private static final Set<String> IN_FLIGHT = Collections.synchronizedSet(new HashSet<>());
    private static long lastRequest;

    private QuestionRefiller() { }

    /**
     * Requests a refill unless one for this key is queued or ran recently.
     * Returns immediately; the caller keeps serving from the bank.
     *
     * @param opentdbCategory OpenTDB category ID, or 0 for any
     * @param difficulty easy, medium, hard, or null for any
     */
    public static void requestRefill(int opentdbCategory, String difficulty) {
        String query = "amount=50&type=multiple&encode=url3986"
                + (opentdbCategory > 0 ? "&category=" + opentdbCategory : "")
                + (difficulty != null ? "&difficulty=" + difficulty : "");
        // Cache key: SHA-256 of provider, endpoint and normalized filters.
        String key = QuestionBank.sha256(PROVIDER + "|" + ENDPOINT + "|" + query);
        if (!IN_FLIGHT.add(key)) return;
        EXECUTOR.submit(() -> {
            try {
                refill(key, query);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                IN_FLIGHT.remove(key);
            }
        });
    }

    private static void refill(String key, String query) throws Exception {
        long now = System.currentTimeMillis();
        long[] state = Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT last_refill, failures FROM question_refills WHERE cache_key = ?")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? new long[]{rs.getLong(1), rs.getLong(2)} : new long[]{0, 0};
                }
            }
        });
        long wait = Math.min(MAX_BACKOFF_MS, MIN_REFILL_INTERVAL_MS << Math.min(state[1], 4));
        if (now - state[0] < wait) return;

        long gap = MIN_REQUEST_GAP_MS - (System.currentTimeMillis() - lastRequest);
        if (gap > 0) Thread.sleep(gap);
        lastRequest = System.currentTimeMillis();

        boolean ok = false;
        try {
            ok = fetchAndStore(query);
        } finally {
            final boolean success = ok;
            Database.inTransaction(c -> {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO question_refills (cache_key, last_refill, "
                        + "last_success, failures) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                        + "last_refill = VALUES(last_refill), last_success = IF(?, VALUES(last_refill), last_success), "
                        + "failures = IF(?, 0, failures + 1)")) {
                    ps.setString(1, key);
                    ps.setLong(2, System.currentTimeMillis());
                    ps.setLong(3, success ? System.currentTimeMillis() : 0);
                    ps.setInt(4, success ? 0 : 1);
                    ps.setBoolean(5, success);
                    ps.setBoolean(6, success);
                    ps.executeUpdate();
                }
                return null;
            });
        }
    }

    private static boolean fetchAndStore(String query) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(ENDPOINT + "?" + query).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "gryph-discord-bot");
        if (conn.getResponseCode() != 200) return false;
        StringBuilder body = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) body.append(line);
        }
        JSONObject json = new JSONObject(body.toString());
        if (json.getInt("response_code") != 0) return false; // rate limited, empty, or bad parameter
        JSONArray results = json.getJSONArray("results");
        Random random = new Random();
        for (int i = 0; i < results.length(); i++) {
            JSONObject q = results.getJSONObject(i);
            List<String> choices = new ArrayList<>();
            String correct = decode(q.getString("correct_answer"));
            choices.add(correct);
            JSONArray wrong = q.getJSONArray("incorrect_answers");
            for (int j = 0; j < wrong.length(); j++) choices.add(decode(wrong.getString(j)));
            Collections.shuffle(choices, random);
            QuestionBank.insert(PROVIDER, "trivia", decode(q.getString("category")), q.getString("difficulty"),
                    decode(q.getString("question")), choices, choices.indexOf(correct), null,
                    "CC BY-SA 4.0", "Open Trivia DB (opentdb.com)");
        }
        return true;
    }

    private static String decode(String s) throws Exception {
        return URLDecoder.decode(s, "UTF-8");
    }

    // ------------------------------------------------------------------ other sources

    private static final String SECOND_PROVIDER = "thetriviaapi";
    private static final String SECOND_ENDPOINT = "https://the-trivia-api.com/v2/questions?limit=50";

    /**
     * Optional second trivia provider. Its free data is CC BY-NC 4.0, so it only runs when the
     * operator confirms noncommercial use with {@code -Dtrivia.noncommercial=true}.
     */
    public static void requestSecondaryRefill() {
        if (!Boolean.getBoolean("trivia.noncommercial")) return;
        String key = QuestionBank.sha256(SECOND_PROVIDER + "|" + SECOND_ENDPOINT);
        if (!IN_FLIGHT.add(key)) return;
        EXECUTOR.submit(() -> {
            try {
                guarded(key, () -> {
                    String body = httpGet(SECOND_ENDPOINT, null);
                    if (body == null) return false;
                    store(QuestionSources.parseTheTriviaApi(body, new Random()), SECOND_PROVIDER, "CC BY-NC 4.0",
                            "The Trivia API (the-trivia-api.com)");
                    return true;
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                IN_FLIGHT.remove(key);
            }
        });
    }

    /**
     * Syncs approved puzzle packs from a public GitHub repository, set with {@code -Dpuzzles.repo=owner/name}
     * (and optionally {@code -Dpuzzles.path=packs}). A file is imported once per content version.
     */
    public static void requestPackSync() {
        String repo = System.getProperty("puzzles.repo");
        if (repo == null || repo.isEmpty()) return;
        String path = System.getProperty("puzzles.path", "packs");
        String listKey = QuestionBank.sha256("github|" + repo + "|" + path);
        if (!IN_FLIGHT.add(listKey)) return;
        EXECUTOR.submit(() -> {
            try {
                String token = System.getenv("GITHUB_TOKEN");
                String listing = httpGet("https://api.github.com/repos/" + repo + "/contents/" + path, token);
                if (listing == null) return;
                JSONArray files = new JSONArray(listing);
                for (int i = 0; i < files.length(); i++) {
                    JSONObject f = files.getJSONObject(i);
                    if (!f.getString("name").endsWith(".json") || f.isNull("download_url")) continue;
                    String versionKey = QuestionBank.sha256("github|" + repo + "|" + f.getString("path") + "|"
                            + f.getString("sha"));
                    if (alreadyImported(versionKey)) continue;
                    String body = httpGet(f.getString("download_url"), token);
                    if (body == null) continue;
                    store(QuestionSources.parsePack(body), "github:" + repo, "Project content",
                            "Puzzle pack " + f.getString("name") + " from " + repo);
                    markImported(versionKey);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                IN_FLIGHT.remove(listKey);
            }
        });
    }

    private interface Fetch {
        boolean run() throws Exception;
    }

    /** Applies the same interval and failure backoff as the primary provider. */
    private static void guarded(String key, Fetch fetch) throws Exception {
        long now = System.currentTimeMillis();
        long[] state = Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT last_refill, failures FROM question_refills WHERE cache_key = ?")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? new long[]{rs.getLong(1), rs.getLong(2)} : new long[]{0, 0};
                }
            }
        });
        if (now - state[0] < Math.min(MAX_BACKOFF_MS, MIN_REFILL_INTERVAL_MS << Math.min(state[1], 4))) return;
        boolean ok = false;
        try {
            ok = fetch.run();
        } finally {
            final boolean success = ok;
            Database.inTransaction(c -> {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO question_refills (cache_key, last_refill, "
                        + "last_success, failures) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                        + "last_refill = VALUES(last_refill), failures = IF(?, 0, failures + 1)")) {
                    ps.setString(1, key);
                    ps.setLong(2, System.currentTimeMillis());
                    ps.setLong(3, success ? System.currentTimeMillis() : 0);
                    ps.setInt(4, success ? 0 : 1);
                    ps.setBoolean(5, success);
                    ps.executeUpdate();
                }
                return null;
            });
        }
    }

    private static void store(List<QuestionSources.Parsed> parsed, String provider, String license,
                              String attribution) throws Exception {
        for (QuestionSources.Parsed p : parsed) {
            QuestionBank.insert(provider, p.kind, p.category, p.difficulty, p.prompt, p.choices, p.correct,
                    p.explanation, license, attribution);
        }
    }

    private static boolean alreadyImported(String key) throws Exception {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM question_refills WHERE cache_key = ? AND last_success > 0")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    private static void markImported(String key) throws Exception {
        Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO question_refills (cache_key, last_refill, "
                    + "last_success, failures) VALUES (?, ?, ?, 0) ON DUPLICATE KEY UPDATE last_success = "
                    + "VALUES(last_success)")) {
                ps.setString(1, key);
                ps.setLong(2, System.currentTimeMillis());
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            }
            return null;
        });
    }

    private static String httpGet(String url, String bearer) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "gryph-discord-bot");
        if (bearer != null) conn.setRequestProperty("Authorization", "Bearer " + bearer);
        if (conn.getResponseCode() != 200) return null;
        StringBuilder body = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) body.append(line).append('\n');
        }
        return body.toString();
    }
}

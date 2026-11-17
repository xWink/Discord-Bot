package economy;

import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * One-time unlocks. Awarding is best-effort: a failure never blocks the
 * action that earned it. Each unlock counts once on the collection board.
 */
public final class Achievements {

    public static final Map<String, String> ALL = new LinkedHashMap<>();

    static {
        ALL.put("first-correct", "Answered an arcade question correctly");
        ALL.put("punch-card", "Completed a weekly punch card");
        ALL.put("duel-win", "Won a duel");
        ALL.put("blackjack-win", "Won a hand of blackjack");
        ALL.put("goal-helper", "Contributed to a community goal");
        ALL.put("bounty-worker", "Got paid for a bounty");
        ALL.put("gauntlet-clear", "Cleared the puzzle gauntlet");
        ALL.put("event-goer", "Attended a hosted event");
        ALL.put("weekly-done", "Finished a weekly challenge");
    }

    private Achievements() { }

    /**
     * @return true if newly unlocked
     */
    public static boolean award(long user, String code) {
        if (!ALL.containsKey(code)) throw new IllegalArgumentException("Unknown achievement " + code);
        try {
            return Database.inTransaction(c -> awardIn(c, user, code));
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Award inside a caller-owned transaction. */
    public static boolean awardIn(java.sql.Connection c, long user, String code) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT IGNORE INTO achievements (user, code, unlocked_at) VALUES (?, ?, ?)")) {
            ps.setLong(1, user);
            ps.setString(2, code);
            ps.setLong(3, System.currentTimeMillis());
            return ps.executeUpdate() > 0;
        }
    }

    public static List<String> unlocked(long user) throws SQLException {
        return Database.inTransaction(c -> {
            List<String> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT code FROM achievements WHERE user = ? ORDER BY unlocked_at")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(rs.getString(1));
                }
            }
            return list;
        });
    }
}

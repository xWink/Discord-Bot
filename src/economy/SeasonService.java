package economy;

import database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Monthly seasons and the boards behind them. Completed seasons are
 * snapshotted so results stay browsable, season winners get a temporary
 * badge, and players can opt out of public boards without losing personal
 * stats. Wallet balances, total wagers and total losses are never ranked.
 */
public final class SeasonService {

    public static final String[] BOARDS = {"arcade", "duel", "collection", "community"};
    public static final int TOP = 10;
    private static final ZoneId ZONE = ZoneId.of("America/New_York");
    private static final long BADGE_MS = 30L * 24 * 60 * 60 * 1000;
    private static final String SETTING = "season_snapshotted";

    private SeasonService() { }

    /** One ranked row. */
    public static final class Row {
        public final long user;
        public final int score;
        public final int rank;

        Row(long user, int score, int rank) {
            this.user = user;
            this.score = score;
            this.rank = rank;
        }
    }

    public static int seasonOf(LocalDate date) {
        return date.getYear() * 100 + date.getMonthValue();
    }

    public static int currentSeason() {
        return seasonOf(LocalDate.now(ZONE));
    }

    public static int previousSeason() {
        return seasonOf(LocalDate.now(ZONE).minusMonths(1).withDayOfMonth(1));
    }

    private static long monthStart(int season) {
        return LocalDate.of(season / 100, season % 100, 1).atStartOfDay(ZONE).toInstant().toEpochMilli();
    }

    private static long monthEnd(int season) {
        return LocalDate.of(season / 100, season % 100, 1).plusMonths(1).atStartOfDay(ZONE).toInstant()
                .toEpochMilli();
    }

    /**
     * Live ranking for a board. For the arcade it covers the given season's month.
     */
    public static List<Row> live(String board, int season) throws SQLException {
        return Database.inTransaction(c -> ranking(c, board, season));
    }

    /** Stored ranking of a finished season. */
    public static List<Row> snapshot(String board, int season) throws SQLException {
        return Database.inTransaction(c -> {
            List<Row> rows = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT user, score, rank FROM season_snapshots "
                    + "WHERE season = ? AND board = ? ORDER BY rank")) {
                ps.setInt(1, season);
                ps.setString(2, board);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) rows.add(new Row(rs.getLong(1), rs.getInt(2), rs.getInt(3)));
                }
            }
            return rows;
        });
    }

    private static List<Row> ranking(Connection c, String board, int season) throws SQLException {
        String sql;
        switch (board) {
            case "arcade":
                sql = "SELECT user, CAST(SUM(correct) AS SIGNED) s FROM arcade_scores WHERE played_at >= ? "
                        + "AND played_at < ? AND user NOT IN (SELECT user FROM board_optout) GROUP BY user "
                        + "HAVING s > 0 ORDER BY s DESC, user";
                break;
            case "duel":
                sql = "SELECT user, rating s FROM duel_ratings WHERE matches >= 5 AND ? < ? "
                        + "AND user NOT IN (SELECT user FROM board_optout) ORDER BY s DESC, user";
                break;
            case "collection":
                sql = "SELECT user, COUNT(DISTINCT item_code) s FROM inventory WHERE ? < ? "
                        + "AND user NOT IN (SELECT user FROM board_optout) GROUP BY user ORDER BY s DESC, user";
                break;
            case "community":
                sql = "SELECT c.user, COUNT(DISTINCT c.goal_id) s FROM goal_contributions c JOIN community_goals g "
                        + "ON g.id = c.goal_id WHERE g.status = 'REACHED' AND c.reversed = 0 AND ? < ? "
                        + "AND c.user NOT IN (SELECT user FROM board_optout) GROUP BY c.user ORDER BY s DESC, c.user";
                break;
            default:
                throw new IllegalArgumentException("Unknown board " + board);
        }
        List<Row> rows = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, board.equals("arcade") ? monthStart(season) : 0);
            ps.setLong(2, board.equals("arcade") ? monthEnd(season) : 1);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) rows.add(new Row(rs.getLong(1), rs.getInt(2), rank++));
            }
        }
        return rows;
    }

    public static boolean isOptedOut(long user) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM board_optout WHERE user = ?")) {
                ps.setLong(1, user);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public static void setOptOut(long user, boolean out) throws SQLException {
        Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(out ? "INSERT IGNORE INTO board_optout (user) VALUES (?)"
                    : "DELETE FROM board_optout WHERE user = ?")) {
                ps.setLong(1, user);
                ps.executeUpdate();
            }
            return null;
        });
    }

    /**
     * Snapshots a finished season once, awards its winners a 30-day badge, and
     * returns who won. Running it again for the same season changes nothing.
     */
    public static Set<Long> closeSeason(int season) throws SQLException {
        return Database.inTransaction(c -> {
            Set<Long> winners = new LinkedHashSet<>();
            for (String board : BOARDS) {
                if (board.equals("arcade") || board.equals("duel")) {
                    List<Row> rows = ranking(c, board, season);
                    for (Row r : rows) {
                        if (r.rank > TOP) break;
                        try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO season_snapshots "
                                + "(season, board, rank, user, score) VALUES (?, ?, ?, ?, ?)")) {
                            ps.setInt(1, season);
                            ps.setString(2, board);
                            ps.setInt(3, r.rank);
                            ps.setLong(4, r.user);
                            ps.setInt(5, r.score);
                            ps.executeUpdate();
                        }
                    }
                }
            }
            for (String board : new String[]{"arcade", "duel"}) {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT user FROM season_snapshots WHERE season = ? AND board = ? AND rank = 1")) {
                    ps.setInt(1, season);
                    ps.setString(2, board);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) continue;
                        long user = rs.getLong(1);
                        winners.add(user);
                        try (PreparedStatement ins = c.prepareStatement("INSERT IGNORE INTO inventory (user, "
                                + "item_code, purchase_id, expires_at, created_at) VALUES (?, ?, ?, ?, ?)")) {
                            ins.setLong(1, user);
                            ins.setString(2, "season-" + season + "-" + board);
                            ins.setString(3, "season:" + season + ":" + board + ":" + user);
                            ins.setLong(4, System.currentTimeMillis() + BADGE_MS);
                            ins.setLong(5, System.currentTimeMillis());
                            ins.executeUpdate();
                        }
                    }
                }
            }
            return winners;
        });
    }

    /**
     * @return the winners of the most recent snapshotted season, i.e. the full
     *         current winner list the champion role must match
     */
    public static Set<Long> currentWinners() throws SQLException {
        return Database.inTransaction(c -> {
            Set<Long> winners = new LinkedHashSet<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT DISTINCT user FROM season_snapshots WHERE rank = 1 "
                    + "AND season = (SELECT MAX(season) FROM season_snapshots)")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) winners.add(rs.getLong(1));
                }
            }
            return winners;
        });
    }

    /** @return true if the previous season has not been closed yet and this call should close it */
    public static boolean closePreviousIfDue() throws SQLException {
        int prev = previousSeason();
        boolean due = Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT value FROM economy_settings WHERE name = ? FOR UPDATE")) {
                ps.setString(1, SETTING);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getLong(1) >= prev) return false;
                }
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO economy_settings (name, value) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE value = VALUES(value)")) {
                ps.setString(1, SETTING);
                ps.setLong(2, prev);
                ps.executeUpdate();
            }
            return true;
        });
        if (due) closeSeason(prev);
        return due;
    }
}

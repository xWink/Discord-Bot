package arcade;

import database.Database;
import economy.DailyService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Free daily arcade. The featured activity rotates between trivia and
 * code-reading. Every completed round counts toward the shared daily payout
 * and the punch card; replays after the payout stay free and unrewarded.
 */
public final class ArcadeService {

    public static final long ROUND_SECONDS = 45;

    /** An open round awaiting an answer. */
    public static final class Round {
        public final Question question;
        public final long startedAt;

        Round(Question question, long startedAt) {
            this.question = question;
            this.startedAt = startedAt;
        }
    }

    /** Outcome shown after an answer. */
    public static final class Result {
        public final boolean timedOut;
        public final boolean correct;
        public final Question question;
        public final boolean dailyPaid;
        public final boolean punchPaid;

        Result(boolean timedOut, boolean correct, Question question, boolean dailyPaid, boolean punchPaid) {
            this.timedOut = timedOut;
            this.correct = correct;
            this.question = question;
            this.dailyPaid = dailyPaid;
            this.punchPaid = punchPaid;
        }
    }

    private static final Map<Long, Round> ROUNDS = new ConcurrentHashMap<>();

    private ArcadeService() { }

    /**
     * @return "trivia" or "code", alternating by local calendar day
     */
    public static String featuredKind() {
        long day = LocalDate.now(ZoneId.of("America/New_York")).toEpochDay();
        return day % 2 == 0 ? "trivia" : "code";
    }

    /**
     * Opens a round for the player, replacing any unanswered one.
     *
     * @return the round, or null if the bank has nothing of today's kind
     */
    public static Round start(long user) throws SQLException {
        String kind = featuredKind();
        Question q = QuestionBank.next(kind, null, user);
        if (q == null) q = QuestionBank.next(kind.equals("trivia") ? "code" : "trivia", null, user);
        if (q == null) return null;
        if (QuestionBank.stock("trivia", null) < 40) QuestionRefiller.requestRefill(0, null);
        Round round = new Round(q, System.currentTimeMillis());
        ROUNDS.put(user, round);
        return round;
    }

    /**
     * Scores an answer. The round is consumed whether or not it was right.
     *
     * @param choice index of the picked choice
     * @return the result, or null if the player has no open round
     */
    public static Result answer(long user, int choice) throws SQLException {
        Round round = ROUNDS.remove(user);
        if (round == null) return null;
        long now = System.currentTimeMillis();
        long elapsed = now - round.startedAt;
        boolean timedOut = elapsed > ROUND_SECONDS * 1000;
        boolean correct = !timedOut && choice == round.question.getCorrectIndex();

        if (timedOut) return new Result(true, false, round.question, false, false);

        if (rankedToday(user, now) < RANKED_PER_DAY_CAP) {
            record(user, round.question.getId(), correct, (int) elapsed, now);
        }
        // Completing a round, right or wrong, counts as the day's activity.
        boolean daily = DailyService.getInstance().claim(user, "arcade", now);
        boolean punch = PunchCard.record(user);
        return new Result(false, correct, round.question, daily, punch);
    }

    private static int rankedToday(long user, long now) throws SQLException {
        long dayStart = LocalDate.now(ZoneId.of("America/New_York")).atStartOfDay(ZoneId.of("America/New_York"))
                .toInstant().toEpochMilli();
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM arcade_scores WHERE user = ? AND played_at >= ?")) {
                ps.setLong(1, user);
                ps.setLong(2, dayStart);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
    }

    private static void record(long user, long questionId, boolean correct, int elapsed, long now)
            throws SQLException {
        Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO arcade_scores (user, question_id, correct, "
                    + "elapsed_ms, week, played_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setLong(1, user);
                ps.setLong(2, questionId);
                ps.setInt(3, correct ? 1 : 0);
                ps.setInt(4, elapsed);
                ps.setInt(5, PunchCard.currentWeek());
                ps.setLong(6, now);
                ps.executeUpdate();
            }
            return null;
        });
    }

    /** One row of the weekly board. */
    public static final class Standing {
        public final long user;
        public final int correct;
        public final int played;
        public final long avgMs;
        public final int rank;

        Standing(long user, int correct, int played, long avgMs, int rank) {
            this.user = user;
            this.correct = correct;
            this.played = played;
            this.avgMs = avgMs;
            this.rank = rank;
        }
    }

    /**
     * Weekly ranking by correct answers, ties broken by faster average time on
     * correct answers. Only the first {@value #RANKED_PER_DAY_CAP} rounds each day are
     * ranked; further rounds are free practice with no score.
     *
     * @return all standings, best first
     */
    public static List<Standing> weeklyBoard() throws SQLException {
        int week = PunchCard.currentWeek();
        return Database.inTransaction(c -> {
            List<Standing> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT user, SUM(correct) AS right_count, COUNT(*) AS n, "
                    + "COALESCE(AVG(CASE WHEN correct = 1 THEN elapsed_ms END), 0) AS avg_ms FROM (SELECT user, "
                    + "correct, elapsed_ms FROM arcade_scores WHERE week = ? AND user NOT IN (SELECT user FROM board_optout) ORDER BY id) s GROUP BY user "
                    + "ORDER BY right_count DESC, avg_ms ASC, user ASC")) {
                ps.setInt(1, week);
                try (ResultSet rs = ps.executeQuery()) {
                    int rank = 1;
                    while (rs.next()) {
                        list.add(new Standing(rs.getLong(1), rs.getInt(2), rs.getInt(3), rs.getLong(4), rank++));
                    }
                }
            }
            return list;
        });
    }

    public static final int RANKED_PER_DAY_CAP = 10;
}

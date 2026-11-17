package arcade;

import database.Database;
import economy.Achievements;
import economy.EconomyService;
import economy.InsufficientFundsException;
import economy.WeeklyChallenge;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Three-question runs. WEEKLY is the free weekly challenge (once paid per
 * week). The gauntlet mixes trivia and code: PRACTICE is free with a small
 * capped reward, PRIZE costs 3 GC and awards that week's cosmetic for a clear.
 * The entry fee is held as a reservation, so a run the bot cannot finish is
 * refunded.
 */
public final class QuizService {

    public enum Mode { WEEKLY, PRACTICE, PRIZE }

    public static final int ENTRY_FEE = 3;
    public static final int PRACTICE_REWARD = 1;
    public static final int PRACTICE_WEEKLY_CAP = 3;
    public static final int QUESTIONS = 3;
    public static final long RUN_LIMIT_MS = 5 * 60 * 1000;

    private static final Map<Long, Session> SESSIONS = new ConcurrentHashMap<>();

    private QuizService() { }

    /** An open run. */
    public static final class Session {
        public final long user;
        public final Mode mode;
        public final List<Question> questions;
        public final String reservation;
        public final long channelId;
        final long startedAt = System.currentTimeMillis();
        int index;
        int correct;
        long questionStart = System.currentTimeMillis();

        Session(long user, Mode mode, List<Question> questions, String reservation, long channelId) {
            this.user = user;
            this.mode = mode;
            this.questions = questions;
            this.reservation = reservation;
            this.channelId = channelId;
        }

        public Question current() { return questions.get(index); }
        public int number() { return index + 1; }
    }

    /** Result of an answer. */
    public static final class Step {
        public final boolean correct;
        public final boolean timedOut;
        public final Question answered;
        public final Session next;       // null when finished
        public final int correctTotal;
        public final int reward;         // GC paid at the end
        public final boolean prizeAwarded;
        public final boolean feeTaken;

        Step(boolean correct, boolean timedOut, Question answered, Session next, int correctTotal, int reward,
             boolean prizeAwarded, boolean feeTaken) {
            this.correct = correct;
            this.timedOut = timedOut;
            this.answered = answered;
            this.next = next;
            this.correctTotal = correctTotal;
            this.reward = reward;
            this.prizeAwarded = prizeAwarded;
            this.feeTaken = feeTaken;
        }
    }

    public static Session current(long user) {
        return SESSIONS.get(user);
    }

    /**
     * Starts a run.
     *
     * @param kind "trivia" or "code" for the weekly challenge; ignored for the gauntlet
     * @return the session, or a String explaining why not
     */
    public static Object start(long user, Mode mode, String kind, long channelId, String opId)
            throws SQLException {
        if (SESSIONS.containsKey(user)) return "You already have a run open. Finish it with `!answer`.";
        int week = PunchCard.currentWeek();
        if (mode == Mode.WEEKLY) {
            boolean done = Database.inTransaction(c -> WeeklyChallenge.isDoneIn(c, user, week));
            if (done) return "You already completed this week's challenge.";
            if (attempts(user, week, Mode.WEEKLY) >= WeeklyChallenge.MAX_ATTEMPTS) {
                return "You have used both weekly attempts. A new challenge starts Monday.";
            }
        }
        List<Question> qs = new ArrayList<>();
        String[] kinds = mode == Mode.WEEKLY ? new String[]{kind, kind, kind} : new String[]{"trivia", "code", "trivia"};
        for (String k : kinds) {
            Question q = QuestionBank.next(k, null, user);
            for (int tries = 0; q != null && tries < 5 && containsId(qs, q.getId()); tries++) {
                q = QuestionBank.next(k, null, user);
            }
            if (q == null || containsId(qs, q.getId())) return "There are not enough " + k + " questions loaded yet.";
            qs.add(q);
        }
        String reservation = null;
        if (mode == Mode.PRIZE) {
            reservation = "gauntlet:" + opId;
            try {
                EconomyService.getInstance().reserve(user, ENTRY_FEE, "gauntlet", opId, reservation);
            } catch (InsufficientFundsException e) {
                return "The prize run costs " + ENTRY_FEE + " GC and you have " + e.getSpendable() + " spendable.";
            }
        }
        Session s = new Session(user, mode, qs, reservation, channelId);
        if (SESSIONS.putIfAbsent(user, s) != null) {
            if (reservation != null) EconomyService.getInstance().refundReservation(reservation);
            return "You already have a run open.";
        }
        return s;
    }

    private static boolean containsId(List<Question> list, long id) {
        for (Question q : list) if (q.getId() == id) return true;
        return false;
    }

    /** @return null if the player has no open run */
    public static Step answer(long user, int choice) throws SQLException {
        Session s = SESSIONS.get(user);
        if (s == null) return null;
        synchronized (s) {
            if (SESSIONS.get(user) != s) return null;
            Question q = s.current();
            long now = System.currentTimeMillis();
            boolean timedOut = now - s.questionStart > ArcadeService.ROUND_SECONDS * 1000;
            boolean right = !timedOut && choice == q.getCorrectIndex();
            if (right) s.correct++;
            s.index++;
            s.questionStart = now;
            if (s.index < QUESTIONS) return new Step(right, timedOut, q, s, s.correct, 0, false, false);
            return finish(s, q, right, timedOut);
        }
    }

    /** Closes runs left idle past the limit. Paid entries are refunded. */
    public static List<Session> sweep() {
        List<Session> closed = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Session s : new ArrayList<>(SESSIONS.values())) {
            if (now - s.startedAt <= RUN_LIMIT_MS) continue;
            synchronized (s) {
                if (!SESSIONS.remove(s.user, s)) continue;
                try {
                    if (s.reservation != null) EconomyService.getInstance().refundReservation(s.reservation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                closed.add(s);
            }
        }
        return closed;
    }

    private static Step finish(Session s, Question last, boolean lastRight, boolean lastTimedOut) throws SQLException {
        int week = PunchCard.currentWeek();
        boolean clear = s.correct == QUESTIONS;
        int[] reward = {0};
        boolean[] prize = {false};
        Database.inTransaction(c -> {
            reward[0] = 0;
            prize[0] = false;
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO quiz_runs (user, week, mode, correct, total, "
                    + "paid, played_at) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setLong(1, s.user);
                ps.setInt(2, week);
                ps.setString(3, s.mode.name());
                ps.setInt(4, s.correct);
                ps.setInt(5, QUESTIONS);
                ps.setInt(6, s.mode == Mode.PRIZE ? 1 : 0);
                ps.setLong(7, System.currentTimeMillis());
                ps.executeUpdate();
            }
            switch (s.mode) {
                case WEEKLY:
                    if (s.correct >= 2) {
                        Achievements.awardIn(c, s.user, "weekly-done");
                        reward[0] = WeeklyChallenge.payIn(c, s.user, clear ? WeeklyChallenge.PERFECT_REWARD
                                : WeeklyChallenge.BASE_REWARD, "quiz");
                    }
                    break;
                case PRACTICE:
                    if (clear && practicePaid(c, s.user, week) < PRACTICE_WEEKLY_CAP) {
                        EconomyService.creditIn(c, s.user, PRACTICE_REWARD, "gauntlet:practice",
                                Integer.toString(week), "practice:" + s.user + ":" + System.nanoTime());
                        reward[0] = PRACTICE_REWARD;
                    }
                    break;
                default: // PRIZE: the held entry fee is spent, a clear wins the week's cosmetic
                    EconomyService.settleReservationIn(c, s.reservation, -ENTRY_FEE, "gauntlet:entry");
                    if (clear) {
                        try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO inventory (user, item_code, "
                                + "purchase_id, expires_at, created_at) VALUES (?, ?, ?, 0, ?)")) {
                            ps.setLong(1, s.user);
                            ps.setString(2, "gauntlet-w" + week);
                            ps.setString(3, "gauntlet:" + s.user + ":" + week);
                            ps.setLong(4, System.currentTimeMillis());
                            prize[0] = ps.executeUpdate() > 0;
                        }
                        Achievements.awardIn(c, s.user, "gauntlet-clear");
                    }
            }
            return null;
        });
        SESSIONS.remove(s.user, s);
        return new Step(lastRight, lastTimedOut, last, null, s.correct, reward[0], prize[0], s.mode == Mode.PRIZE);
    }

    private static int practicePaid(java.sql.Connection c, long user, int week) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM economy_ledger WHERE user = ? "
                + "AND reason = 'gauntlet:practice' AND ref = ?")) {
            ps.setLong(1, user);
            ps.setString(2, Integer.toString(week));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static int attempts(long user, int week, Mode mode) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM quiz_runs WHERE user = ? AND week = ? AND mode = ?")) {
                ps.setLong(1, user);
                ps.setInt(2, week);
                ps.setString(3, mode.name());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
    }
}

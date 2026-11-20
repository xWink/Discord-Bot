package arcade;

import economy.EconomyService;
import economy.InsufficientFundsException;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A host pays 5-10 GC to pick a category and start a round the whole channel
 * can join for free. Joiners (and the host) get normal arcade credit: score
 * within the daily ranked cap, the shared daily reward, and the punch card.
 * The round pays nothing extra, so the normal reward caps hold.
 */
public final class PersonalTrivia {

    public static final int MIN_COST = 5;
    public static final int MAX_COST = 10;
    public static final long LIFETIME_MS = 60 * 1000;

    private static final Map<Long, Round> BY_CHANNEL = new ConcurrentHashMap<>();

    private PersonalTrivia() { }

    /** An open round in one channel. */
    public static final class Round {
        public final Question question;
        public final long host;
        public final long startedAt = System.currentTimeMillis();
        final Set<Long> answered = new HashSet<>();

        Round(Question question, long host) {
            this.question = question;
            this.host = host;
        }
    }

    /**
     * @return the round, or a String explaining why it could not start
     */
    public static Object start(long host, long channelId, String category, int cost, String opId)
            throws SQLException {
        if (cost < MIN_COST || cost > MAX_COST) return "A round costs " + MIN_COST + " to " + MAX_COST + " GC.";
        if (BY_CHANNEL.containsKey(channelId)) return "This channel already has a round running.";
        Question q = QuestionBank.nextMatching("trivia", category, host);
        if (q == null) return "No trivia matches `" + category + "`. Say `!ptrivia` to list categories.";
        // The question is found before anything is charged, so a failed delivery costs nothing.
        try {
            EconomyService.getInstance().debit(host, cost, "trivia:personal", Long.toString(q.getId()), opId);
        } catch (InsufficientFundsException e) {
            return "You have " + e.getSpendable() + " spendable GC and a round costs " + cost + ".";
        }
        Round round = new Round(q, host);
        if (BY_CHANNEL.putIfAbsent(channelId, round) != null) {
            EconomyService.getInstance().credit(host, cost, "trivia:personal:refund", Long.toString(q.getId()),
                    "refund:" + opId);
            return "This channel already has a round running.";
        }
        return round;
    }

    /**
     * @return the player's result, or null if there is no round, they already answered, or it expired
     */
    public static ArcadeService.Result answer(long channelId, long user, int choice) throws SQLException {
        Round r = BY_CHANNEL.get(channelId);
        if (r == null) return null;
        long now = System.currentTimeMillis();
        if (now - r.startedAt > LIFETIME_MS) {
            BY_CHANNEL.remove(channelId, r);
            return null;
        }
        synchronized (r) {
            if (!r.answered.add(user)) return null;
        }
        return ArcadeService.complete(user, r.question, choice == r.question.getCorrectIndex(),
                (int) (now - r.startedAt), now);
    }

    public static Round current(long channelId) {
        return BY_CHANNEL.get(channelId);
    }

    /** Removes expired rounds; returns their channel IDs and answers for the timer to announce. */
    public static Map<Long, Round> sweep() {
        Map<Long, Round> done = new ConcurrentHashMap<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<Long, Round> e : BY_CHANNEL.entrySet()) {
            if (now - e.getValue().startedAt > LIFETIME_MS && BY_CHANNEL.remove(e.getKey(), e.getValue())) {
                done.put(e.getKey(), e.getValue());
            }
        }
        return done;
    }

    public static int participants(Round r) {
        synchronized (r) {
            return r.answered.size();
        }
    }
}

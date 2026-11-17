package duel;

import arcade.Question;
import arcade.QuestionBank;
import database.Database;
import economy.EconomyService;
import economy.WagerLimits;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Trivia and code duels. Stakes are reserved only once both players have
 * accepted, so an unanswered challenge never holds coins. The pot (minus the
 * fee) goes to the winner; a draw or an undeliverable round refunds both.
 * In-flight duels live in memory; any stakes left by a restart are released at
 * startup.
 */
public final class DuelService {

    private static final AtomicLong IDS = new AtomicLong(System.currentTimeMillis());
    private static final Map<Long, Duel> BY_USER = new ConcurrentHashMap<>();

    private DuelService() { }

    /** What finished duels report back to the chat layer. */
    public static final class Outcome {
        public final Duel duel;
        public final long winner;
        public final boolean ranked;
        public final int[] ratings; // {challenger, opponent} after the match, or null

        Outcome(Duel duel, long winner, boolean ranked, int[] ratings) {
            this.duel = duel;
            this.winner = winner;
            this.ranked = ranked;
            this.ratings = ratings;
        }
    }

    /** @return null if either player is busy */
    public static Duel challenge(long challenger, long opponent, int stake, long channelId) {
        if (challenger == opponent) return null;
        Duel duel = new Duel(IDS.incrementAndGet(), challenger, opponent, stake, channelId);
        if (BY_USER.putIfAbsent(challenger, duel) != null) return null;
        if (BY_USER.putIfAbsent(opponent, duel) != null) {
            BY_USER.remove(challenger, duel);
            return null;
        }
        return duel;
    }

    public static Duel pendingFor(long opponent) {
        Duel d = BY_USER.get(opponent);
        return d != null && !d.isActive() && d.opponent == opponent ? d : null;
    }

    public static Duel activeFor(long user) {
        Duel d = BY_USER.get(user);
        return d != null && d.isActive() ? d : null;
    }

    public static void cancel(Duel duel) {
        BY_USER.remove(duel.challenger, duel);
        BY_USER.remove(duel.opponent, duel);
    }

    /**
     * Accepts a challenge: checks limits, reserves both stakes in one
     * transaction, and deals one question to both players.
     *
     * @return null on success, otherwise the reason it could not start (the duel is cancelled)
     */
    public static String accept(Duel duel) throws SQLException {
        if (duel.stake > 0) {
            for (long user : new long[]{duel.challenger, duel.opponent}) {
                String blocked = WagerLimits.check(user, duel.stake);
                if (blocked != null) {
                    cancel(duel);
                    return "<@" + user + ">: " + blocked;
                }
            }
        }
        String kind = duel.id % 2 == 0 ? "trivia" : "code";
        Question q = QuestionBank.next(kind, null, duel.challenger);
        if (q == null) q = QuestionBank.next(kind.equals("trivia") ? "code" : "trivia", null, duel.challenger);
        if (q == null) {
            cancel(duel);
            return "No questions are loaded yet.";
        }
        if (duel.stake > 0) {
            try {
                Database.inTransaction(c -> {
                    EconomyService.reserveIn(c, duel.challenger, duel.stake, "duel", Long.toString(duel.id),
                            duel.reservationFor(duel.challenger));
                    EconomyService.reserveIn(c, duel.opponent, duel.stake, "duel", Long.toString(duel.id),
                            duel.reservationFor(duel.opponent));
                    return null;
                });
            } catch (EconomyService.InsufficientFundsRuntime e) {
                cancel(duel);
                return "One of you cannot cover a " + duel.stake + " GC stake.";
            }
        }
        duel.question = q;
        duel.startedAt = System.currentTimeMillis();
        return null;
    }

    /**
     * @return the outcome if this answer completed the duel, otherwise null
     */
    public static Outcome answer(long user, int choice) throws SQLException {
        Duel d = activeFor(user);
        if (d == null || !d.submit(user, choice, System.currentTimeMillis())) return null;
        return d.bothAnswered() ? resolve(d) : null;
    }

    /** Called by the timer: drops stale challenges and finishes overdue duels. */
    public static List<Outcome> sweep() {
        List<Outcome> done = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Duel d : new ArrayList<>(BY_USER.values())) {
            try {
                if (!d.isActive() && now - d.createdAt > Duel.ACCEPT_WINDOW_MS) cancel(d);
                else if (d.isActive() && now - d.startedAt > Duel.ANSWER_WINDOW_MS) {
                    Outcome o = resolve(d);
                    if (o != null) done.add(o);
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    refund(d);
                } catch (Exception ignored) { }
            }
        }
        return done;
    }

    /**
     * Settles the duel once: stakes, match record and ratings commit together.
     *
     * @return the outcome, or null if it was already resolved
     */
    static Outcome resolve(Duel duel) throws SQLException {
        synchronized (duel) {
            if (BY_USER.get(duel.challenger) != duel) return null;
            long winner = duel.winner();
            Outcome outcome = Database.inTransaction(c -> {
                if (duel.stake > 0) {
                    if (winner == 0) {
                        EconomyService.refundReservationIn(c, duel.reservationFor(duel.challenger));
                        EconomyService.refundReservationIn(c, duel.reservationFor(duel.opponent));
                    } else {
                        long loser = winner == duel.challenger ? duel.opponent : duel.challenger;
                        EconomyService.settleReservationIn(c, duel.reservationFor(winner),
                                DuelMath.winnerNet(duel.stake), "duel");
                        EconomyService.settleReservationIn(c, duel.reservationFor(loser), -duel.stake, "duel");
                    }
                }
                boolean ranked = pairRankedToday(c, duel) < DuelMath.RANKED_PAIR_DAILY_CAP;
                int[] ratings = null;
                if (ranked) ratings = rate(c, duel, winner);
                record(c, duel, winner, ranked);
                return new Outcome(duel, winner, ranked, ratings);
            });
            cancel(duel);
            return outcome;
        }
    }

    private static void refund(Duel duel) throws SQLException {
        if (duel.stake > 0) {
            Database.inTransaction(c -> {
                EconomyService.refundReservationIn(c, duel.reservationFor(duel.challenger));
                EconomyService.refundReservationIn(c, duel.reservationFor(duel.opponent));
                return null;
            });
        }
        cancel(duel);
    }

    private static int pairRankedToday(Connection c, Duel d) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM duel_matches WHERE ranked = 1 "
                + "AND played_at >= ? AND ((user_a = ? AND user_b = ?) OR (user_a = ? AND user_b = ?))")) {
            ps.setLong(1, System.currentTimeMillis() - 24L * 60 * 60 * 1000);
            ps.setLong(2, d.challenger);
            ps.setLong(3, d.opponent);
            ps.setLong(4, d.opponent);
            ps.setLong(5, d.challenger);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static int[] rate(Connection c, Duel d, long winner) throws SQLException {
        int ra = rating(c, d.challenger);
        int rb = rating(c, d.opponent);
        int na = ra;
        int nb = rb;
        if (winner != 0) {
            int[] r = DuelMath.update(ra, rb, winner == d.challenger);
            na = r[0];
            nb = r[1];
        }
        save(c, d.challenger, na);
        save(c, d.opponent, nb);
        return new int[]{na, nb};
    }

    private static int rating(Connection c, long user) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT rating FROM duel_ratings WHERE user = ? FOR UPDATE")) {
            ps.setLong(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : DuelMath.START_RATING;
            }
        }
    }

    private static void save(Connection c, long user, int rating) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO duel_ratings (user, rating, matches, updated_at) "
                + "VALUES (?, ?, 1, ?) ON DUPLICATE KEY UPDATE rating = VALUES(rating), matches = matches + 1, "
                + "updated_at = VALUES(updated_at)")) {
            ps.setLong(1, user);
            ps.setInt(2, rating);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    private static void record(Connection c, Duel d, long winner, boolean ranked) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO duel_matches (user_a, user_b, stake, winner, "
                + "ranked, played_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setLong(1, d.challenger);
            ps.setLong(2, d.opponent);
            ps.setInt(3, d.stake);
            if (winner == 0) ps.setNull(4, java.sql.Types.BIGINT);
            else ps.setLong(4, winner);
            ps.setInt(5, ranked ? 1 : 0);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    /** One row of the duel board. */
    public static final class Standing {
        public final long user;
        public final int rating;
        public final int matches;

        Standing(long user, int rating, int matches) {
            this.user = user;
            this.rating = rating;
            this.matches = matches;
        }
    }

    /**
     * @return players with at least five rated matches, best first, excluding opted-out players
     */
    public static List<Standing> board() throws SQLException {
        return Database.inTransaction(c -> {
            List<Standing> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT user, rating, matches FROM duel_ratings "
                    + "WHERE matches >= ? AND user NOT IN (SELECT user FROM board_optout) "
                    + "ORDER BY rating DESC, matches DESC, user")) {
                ps.setInt(1, DuelMath.RATED_MATCHES_FOR_RANK);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(new Standing(rs.getLong(1), rs.getInt(2), rs.getInt(3)));
                }
            }
            return list;
        });
    }

    /** Releases stakes orphaned by a restart. */
    public static int releaseOrphans() throws SQLException {
        return EconomyService.getInstance().refundAllByReason("duel");
    }

}

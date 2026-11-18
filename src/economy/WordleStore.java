package economy;

import command.util.game.WordleGame;
import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** One player's board for one day, and the streak across days. Solving pays once, in the same transaction. */
public final class WordleStore {

    private WordleStore() { }

    /** A player's day. */
    public static final class Board {
        public final List<String> guesses;
        public final boolean solved;
        /** GC paid by the guess that just solved it, or 0. */
        public final int paid;

        Board(List<String> guesses, boolean solved, int paid) {
            this.guesses = guesses;
            this.solved = solved;
            this.paid = paid;
        }

        public boolean over() {
            return solved || guesses.size() >= WordleGame.MAX_GUESSES;
        }
    }

    private static List<String> split(String s) {
        return s.isEmpty() ? new ArrayList<String>() : new ArrayList<>(Arrays.asList(s.split(" ")));
    }

    public static Board load(long user, long day) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT guesses, solved FROM wordle_games WHERE user = ? AND day = ?")) {
                ps.setLong(1, user);
                ps.setLong(2, day);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? new Board(split(rs.getString(1)), rs.getInt(2) != 0, 0)
                            : new Board(new ArrayList<String>(), false, 0);
                }
            }
        });
    }

    /**
     * Adds a guess unless the day is already over.
     *
     * @return the board after the guess, or null if the player had already finished today
     */
    public static Board guess(long user, long day, String guess, String answer) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT IGNORE INTO wordle_games (user, day, guesses, solved) VALUES (?, ?, '', 0)")) {
                ps.setLong(1, user);
                ps.setLong(2, day);
                ps.executeUpdate();
            }
            List<String> guesses;
            boolean solved;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT guesses, solved FROM wordle_games WHERE user = ? AND day = ? FOR UPDATE")) {
                ps.setLong(1, user);
                ps.setLong(2, day);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    guesses = split(rs.getString(1));
                    solved = rs.getInt(2) != 0;
                }
            }
            if (solved || guesses.size() >= WordleGame.MAX_GUESSES) return null;
            guesses.add(guess);
            boolean nowSolved = guess.equals(answer);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE wordle_games SET guesses = ?, solved = ? WHERE user = ? AND day = ?")) {
                ps.setString(1, String.join(" ", guesses));
                ps.setInt(2, nowSolved ? 1 : 0);
                ps.setLong(3, user);
                ps.setLong(4, day);
                ps.executeUpdate();
            }
            int paid = 0;
            if (nowSolved) {
                paid = WordleGame.REWARD;
                EconomyService.creditIn(c, user, paid, "wordle", Long.toString(day), "wordle:" + day + ":" + user);
            }
            return new Board(guesses, nowSolved, paid);
        });
    }

    /** @return how many days in a row the player has solved, counting back from today (or yesterday, if today is open) */
    public static int streak(long user, long today) throws SQLException {
        return Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT day FROM wordle_games WHERE user = ? AND solved = 1 AND day <= ? ORDER BY day DESC LIMIT 400")) {
                ps.setLong(1, user);
                ps.setLong(2, today);
                try (ResultSet rs = ps.executeQuery()) {
                    int streak = 0;
                    long expect = -1;
                    while (rs.next()) {
                        long day = rs.getLong(1);
                        if (expect == -1) {
                            if (day != today && day != today - 1) return 0;
                        } else if (day != expect) {
                            break;
                        }
                        streak++;
                        expect = day - 1;
                    }
                    return streak;
                }
            }
        });
    }
}

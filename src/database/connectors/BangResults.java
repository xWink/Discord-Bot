package database.connectors;

import database.Database;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Timestamped record of every bang attempt, the data behind weekly and
 * seasonal boards. Lifetime counters in the bang table are left untouched.
 */
public final class BangResults {

    private BangResults() { }

    public static void record(long user, long playedAt, boolean died, boolean jammed) throws SQLException {
        Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO bang_results (user, played_at, died, jammed) VALUES (?, ?, ?, ?)")) {
                ps.setLong(1, user);
                ps.setLong(2, playedAt);
                ps.setInt(3, died ? 1 : 0);
                ps.setInt(4, jammed ? 1 : 0);
                ps.executeUpdate();
            }
            return null;
        });
    }
}

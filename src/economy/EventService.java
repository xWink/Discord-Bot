package economy;

import database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Hosted events. A host (staff, or a player who spends an event hosting pass)
 * confirms who took part; ending the event pays each participant 5 GC, capped
 * at 10 GC per participant per week.
 */
public final class EventService {

    public static final int PAYOUT = 5;
    public static final int WEEKLY_CAP = 10;
    public static final String PASS = "event-pass";

    private static final EventService INSTANCE = new EventService();

    private EventService() { }

    public static EventService getInstance() {
        return INSTANCE;
    }

    /** What ending an event paid. */
    public static final class Result {
        public final int participants;
        public final int totalPaid;

        Result(int participants, int totalPaid) {
            this.participants = participants;
            this.totalPaid = totalPaid;
        }
    }

    /**
     * Starts an event.
     *
     * @param staff true if the host is staff and needs no pass
     * @return the event ID, -1 if the host already has one running, -2 if no pass is available
     */
    public long start(long host, String name, boolean staff) throws SQLException {
        return Database.inTransaction(c -> {
            EconomyService.lockWallet(c, host); // serialize this host's starts and pass use
            if (activeId(c, host) > 0) return -1L;
            if (!staff) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM inventory WHERE user = ? "
                        + "AND item_code = ? ORDER BY id LIMIT 1")) {
                    ps.setLong(1, host);
                    ps.setString(2, PASS);
                    if (ps.executeUpdate() == 0) return -2L;
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO events (host, name, status, created_at) VALUES (?, ?, 'ACTIVE', ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, host);
                ps.setString(2, name);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            }
        });
    }

    /** @return how many were newly confirmed, or -1 if the host has no active event */
    public int confirm(long host, List<Long> users) throws SQLException {
        return Database.inTransaction(c -> {
            long id = activeId(c, host);
            if (id < 0) return -1;
            int added = 0;
            for (long user : users) {
                if (user == host) continue;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT IGNORE INTO event_participants (event_id, user) VALUES (?, ?)")) {
                    ps.setLong(1, id);
                    ps.setLong(2, user);
                    added += ps.executeUpdate();
                }
            }
            return added;
        });
    }

    /** Ends the event and pays participants within their weekly cap; null if none was running. */
    public Result end(long host) throws SQLException {
        return Database.inTransaction(c -> {
            long id = activeId(c, host);
            if (id < 0) return null;
            List<Long> users = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT user FROM event_participants WHERE event_id = ? "
                    + "ORDER BY user")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) users.add(rs.getLong(1));
                }
            }
            long weekStart = LocalDate.now(ZoneId.of("America/New_York"))
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay(ZoneId.of("America/New_York")).toInstant().toEpochMilli();
            int total = 0;
            for (long user : users) {
                EconomyService.lockWallet(c, user);
                int paidThisWeek;
                try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(delta), 0) FROM economy_ledger "
                        + "WHERE user = ? AND reason = 'event' AND created_at >= ?")) {
                    ps.setLong(1, user);
                    ps.setLong(2, weekStart);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        paidThisWeek = rs.getInt(1);
                    }
                }
                int pay = Math.min(PAYOUT, WEEKLY_CAP - paidThisWeek);
                if (pay > 0) {
                    EconomyService.creditIn(c, user, pay, "event", Long.toString(id), "event:" + id + ":" + user);
                    total += pay;
                }
                WeeklyChallenge.completeViaEventIn(c, user);
                Achievements.awardIn(c, user, "event-goer");
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE events SET status = 'ENDED' WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            return new Result(users.size(), total);
        });
    }

    private static long activeId(Connection c, long host) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM events WHERE host = ? AND status = 'ACTIVE' LIMIT 1")) {
            ps.setLong(1, host);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        }
    }
}

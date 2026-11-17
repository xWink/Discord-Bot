package economy;

import database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The only place wallet balances change. Every mutation runs in one database
 * transaction, locks the affected wallet rows, checks funds at debit time, and
 * writes a ledger row. Each operation carries an ID: repeating an ID for the
 * same wallet returns the original result instead of applying it twice.
 */
public final class EconomyService {

    public static final int STARTING_BALANCE = 5;

    private static final EconomyService INSTANCE = new EconomyService();

    private EconomyService() { }

    public static EconomyService getInstance() {
        return INSTANCE;
    }

    /**
     * @return a fresh unique operation ID for callers with no natural key
     */
    public static String newOpId() {
        return UUID.randomUUID().toString();
    }

    // ----------------------------------------------------------------- reads

    public int getBalance(long user) throws SQLException {
        return Database.inTransaction(c -> lockWallet(c, user));
    }

    public int getReserved(long user) throws SQLException {
        return Database.inTransaction(c -> reservedTotal(c, user));
    }

    public int getSpendable(long user) throws SQLException {
        return Database.inTransaction(c -> lockWallet(c, user) - reservedTotal(c, user));
    }

    public List<LedgerEntry> getHistory(long user, int limit) throws SQLException {
        return Database.inTransaction(c -> {
            List<LedgerEntry> entries = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT id, op_id, delta, balance_after, reason, ref, "
                    + "created_at FROM economy_ledger WHERE user = ? ORDER BY id DESC LIMIT ?")) {
                ps.setLong(1, user);
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add(new LedgerEntry(rs.getLong(1), rs.getString(2), rs.getInt(3),
                                rs.getInt(4), rs.getString(5), rs.getString(6), rs.getLong(7)));
                    }
                }
            }
            return entries;
        });
    }

    /**
     * Looks a receipt up by operation ID for admin investigation.
     */
    public List<LedgerEntry> findByOpId(String opId) throws SQLException {
        return Database.inTransaction(c -> {
            List<LedgerEntry> entries = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT id, op_id, delta, balance_after, reason, ref, "
                    + "created_at, user FROM economy_ledger WHERE op_id = ?")) {
                ps.setString(1, opId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add(new LedgerEntry(rs.getLong(1), rs.getString(2), rs.getInt(3),
                                rs.getInt(4), rs.getString(5), rs.getString(6), rs.getLong(7)));
                    }
                }
            }
            return entries;
        });
    }

    // ------------------------------------------------------------- mutations

    /**
     * Adds coins to a wallet.
     *
     * @return the wallet balance after the credit
     */
    public int credit(long user, int amount, String reason, String ref, String opId) throws SQLException {
        requirePositive(amount);
        return Database.inTransaction(c -> creditIn(c, user, amount, reason, ref, opId));
    }

    /**
     * Removes spendable coins from a wallet.
     *
     * @return the wallet balance after the debit
     * @throws InsufficientFundsException if the spendable balance is too low
     */
    public int debit(long user, int amount, String reason, String ref, String opId)
            throws SQLException, InsufficientFundsException {
        requirePositive(amount);
        try {
            return Database.inTransaction(c -> debitIn(c, user, amount, reason, ref, opId));
        } catch (InsufficientFundsRuntime e) {
            throw e.unwrap();
        }
    }

    /**
     * Moves coins between two wallets in one transaction. Either both sides
     * commit or neither does.
     */
    public void transfer(long from, long to, int amount, String reason, String ref, String opId)
            throws SQLException, InsufficientFundsException {
        requirePositive(amount);
        if (from == to) throw new IllegalArgumentException("Cannot transfer to the same wallet");
        try {
            Database.inTransaction(c -> {
                // Lock in a fixed order so two opposite transfers cannot deadlock.
                if (from < to) { lockWallet(c, from); lockWallet(c, to); }
                else { lockWallet(c, to); lockWallet(c, from); }
                debitIn(c, from, amount, reason + ":out", ref, opId);
                creditIn(c, to, amount, reason + ":in", ref, opId);
                return null;
            });
        } catch (InsufficientFundsRuntime e) {
            throw e.unwrap();
        }
    }

    /**
     * Transfer plus a separate fee, all in one transaction. The recipient always gets the full
     * {@code amount}; the fee leaves circulation.
     */
    public void transferWithFee(long from, long to, int amount, int fee, String reason, String ref, String opId)
            throws SQLException, InsufficientFundsException {
        requirePositive(amount);
        try {
            Database.inTransaction(c -> {
                if (from < to) { lockWallet(c, from); lockWallet(c, to); }
                else { lockWallet(c, to); lockWallet(c, from); }
                int spendable = lockWallet(c, from) - reservedTotal(c, from);
                if (spendable < amount + fee) throw new InsufficientFundsRuntime(spendable, amount + fee);
                debitIn(c, from, amount, reason + ":out", ref, opId);
                if (fee > 0) debitIn(c, from, fee, reason + ":wrap", ref, opId + ":wrap");
                creditIn(c, to, amount, reason + ":in", ref, opId);
                return null;
            });
        } catch (InsufficientFundsRuntime e) {
            throw e.unwrap();
        }
    }

    /**
     * Holds coins aside so they cannot be spent while a game or purchase is
     * open. Idempotent on the reservation ID.
     */
    public void reserve(long user, int amount, String reason, String ref, String reservationId)
            throws SQLException, InsufficientFundsException {
        requirePositive(amount);
        try {
            Database.inTransaction(c -> {
                reserveIn(c, user, amount, reason, ref, reservationId);
                return null;
            });
        } catch (InsufficientFundsRuntime e) {
            throw e.unwrap();
        }
    }

    /**
     * Reserve inside a caller-owned transaction so a game row and its stake
     * hold commit or roll back together. Throws {@link InsufficientFundsRuntime}.
     */
    public static void reserveIn(Connection c, long user, int amount, String reason, String ref,
                                 String reservationId) throws SQLException {
        int wallet = lockWallet(c, user);
        if (reservationExists(c, reservationId)) return;
        int spendable = wallet - reservedTotal(c, user);
        if (spendable < amount) throw new InsufficientFundsRuntime(spendable, amount);
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO economy_reservations "
                + "(reservation_id, user, amount, reason, ref, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, reservationId);
            ps.setLong(2, user);
            ps.setInt(3, amount);
            ps.setString(4, reason);
            ps.setString(5, ref);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    /**
     * Closes a reservation and applies the outcome. {@code net} is the change
     * to the wallet: positive for a win, zero for a push, and at most the
     * negative of the held stake for a loss. The reservation is consumed in the
     * same transaction, so settling twice is a no-op.
     *
     * @return true if this call settled it, false if it was already closed
     */
    public boolean settleReservation(String reservationId, int net, String reason) throws SQLException {
        return Database.inTransaction(c -> settleReservationIn(c, reservationId, net, reason));
    }

    /** Settlement inside a caller-owned transaction. */
    public static boolean settleReservationIn(Connection c, String reservationId, int net, String reason)
            throws SQLException {
        long user;
        int held;
        String ref;
        try (PreparedStatement ps = c.prepareStatement("SELECT user, amount, ref FROM economy_reservations "
                + "WHERE reservation_id = ? FOR UPDATE")) {
            ps.setString(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                user = rs.getLong(1);
                held = rs.getInt(2);
                ref = rs.getString(3);
            }
        }
        if (net < -held) throw new IllegalArgumentException("Loss exceeds the reserved stake");
        lockWallet(c, user);
        deleteReservation(c, reservationId);
        if (net > 0) creditIn(c, user, net, reason, ref, "settle:" + reservationId);
        else if (net < 0) debitRaw(c, user, -net, reason, ref, "settle:" + reservationId);
        DecayService.collectDueIn(c, user, held, reservationId);
        return true;
    }

    /**
     * Releases a reservation with no wallet change (cancelled or unrecoverable).
     *
     * @return true if a reservation was released
     */
    public boolean refundReservation(String reservationId) throws SQLException {
        return Database.inTransaction(c -> refundReservationIn(c, reservationId));
    }

    /**
     * Releases every open reservation of a reason. Used at startup for
     * in-memory games that cannot be resumed.
     *
     * @return how many were released
     */
    public int refundAllByReason(String reason) throws SQLException {
        return Database.inTransaction(c -> {
            List<String> ids = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT reservation_id FROM economy_reservations WHERE reason = ?")) {
                ps.setString(1, reason);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) ids.add(rs.getString(1));
                }
            }
            for (String id : ids) refundReservationIn(c, id);
            return ids.size();
        });
    }

    /** Release inside a caller-owned transaction. */
    public static boolean refundReservationIn(Connection c, String reservationId) throws SQLException {
        long user = 0;
        int held = 0;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT user, amount FROM economy_reservations WHERE reservation_id = ? FOR UPDATE")) {
            ps.setString(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                user = rs.getLong(1);
                held = rs.getInt(2);
            }
        }
        lockWallet(c, user);
        deleteReservation(c, reservationId);
        DecayService.collectDueIn(c, user, held, reservationId);
        return true;
    }

    // -------------------------------------------------- transaction primitives

    /**
     * Ensures the wallet row exists, locks it, and returns the balance.
     * Safe to call from other transactional code in this package or via
     * {@link #creditIn} for features that need to combine their own writes
     * with a payout atomically.
     */
    public static int lockWallet(Connection c, long user) throws SQLException {
        // Create the row only when missing, so the common path takes just the row lock.
        boolean exists;
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM economy WHERE user = ?")) {
            ps.setLong(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                exists = rs.next();
            }
        }
        if (!exists) {
            try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO economy "
                    + "(user, wallet, role_expiry, role_colour) VALUES (?, ?, 0, NULL)")) {
                ps.setLong(1, user);
                ps.setInt(2, STARTING_BALANCE);
                ps.executeUpdate();
            }
        }
        int wallet;
        int lastWeek;
        try (PreparedStatement ps = c.prepareStatement("SELECT wallet, last_decay_week "
                + "FROM economy WHERE user = ? FOR UPDATE")) {
            ps.setLong(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                wallet = rs.getInt(1);
                lastWeek = rs.getInt(2);
            }
        }
        // Overdue decay is settled before any balance is read or changed.
        return DecayService.settleIn(c, user, wallet, lastWeek, System.currentTimeMillis());
    }

    public static int reservedTotal(Connection c, long user) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(amount), 0) FROM economy_reservations WHERE user = ?")) {
            ps.setLong(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Credit inside a caller-owned transaction. Idempotent on (opId, user). */
    public static int creditIn(Connection c, long user, int amount, String reason, String ref, String opId)
            throws SQLException {
        int wallet = lockWallet(c, user);
        Integer prior = priorResult(c, opId, user);
        if (prior != null) return prior;
        return applyDelta(c, user, wallet, amount, reason, ref, opId);
    }

    /** Debit of spendable funds inside a caller-owned transaction. */
    public static int debitIn(Connection c, long user, int amount, String reason, String ref, String opId)
            throws SQLException {
        int wallet = lockWallet(c, user);
        Integer prior = priorResult(c, opId, user);
        if (prior != null) return prior;
        int spendable = wallet - reservedTotal(c, user);
        if (spendable < amount) throw new InsufficientFundsRuntime(spendable, amount);
        return applyDelta(c, user, wallet, -amount, reason, ref, opId);
    }

    /** Debit that ignores reservations; used only when settling a reservation. */
    private static int debitRaw(Connection c, long user, int amount, String reason, String ref, String opId)
            throws SQLException {
        int wallet = lockWallet(c, user);
        Integer prior = priorResult(c, opId, user);
        if (prior != null) return prior;
        if (wallet < amount) throw new InsufficientFundsRuntime(wallet, amount);
        return applyDelta(c, user, wallet, -amount, reason, ref, opId);
    }

    private static Integer priorResult(Connection c, String opId, long user) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT balance_after FROM economy_ledger WHERE op_id = ? AND user = ?")) {
            ps.setString(1, opId);
            ps.setLong(2, user);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    static int applyDelta(Connection c, long user, int wallet, int delta, String reason, String ref,
                                  String opId) throws SQLException {
        int after = wallet + delta;
        try (PreparedStatement ps = c.prepareStatement("UPDATE economy SET wallet = ? WHERE user = ?")) {
            ps.setInt(1, after);
            ps.setLong(2, user);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO economy_ledger "
                + "(op_id, user, delta, balance_after, reason, ref, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, opId);
            ps.setLong(2, user);
            ps.setInt(3, delta);
            ps.setInt(4, after);
            ps.setString(5, reason);
            ps.setString(6, ref);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
        }
        return after;
    }

    private static boolean reservationExists(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM economy_reservations WHERE reservation_id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean deleteReservation(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM economy_reservations WHERE reservation_id = ?")) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static void requirePositive(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
    }

    /**
     * Unchecked carrier so insufficient-funds failures roll the transaction
     * back from inside a lambda, then surface as the checked exception.
     */
    public static final class InsufficientFundsRuntime extends RuntimeException {
        private final int spendable;
        private final int needed;

        public InsufficientFundsRuntime(int spendable, int needed) {
            super("Insufficient funds");
            this.spendable = spendable;
            this.needed = needed;
        }

        InsufficientFundsException unwrap() {
            return new InsufficientFundsException(spendable, needed);
        }
    }
}

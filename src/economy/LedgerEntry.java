package economy;

/**
 * One immutable row of the wallet history.
 */
public final class LedgerEntry {

    private final long id;
    private final String opId;
    private final int delta;
    private final int balanceAfter;
    private final String reason;
    private final String ref;
    private final long createdAt;

    public LedgerEntry(long id, String opId, int delta, int balanceAfter, String reason, String ref, long createdAt) {
        this.id = id;
        this.opId = opId;
        this.delta = delta;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
        this.ref = ref;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public String getOpId() { return opId; }
    public int getDelta() { return delta; }
    public int getBalanceAfter() { return balanceAfter; }
    public String getReason() { return reason; }
    public String getRef() { return ref; }
    public long getCreatedAt() { return createdAt; }
}

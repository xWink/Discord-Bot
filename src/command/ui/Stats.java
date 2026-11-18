package command.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/** What the bot has been doing since it started: commands run, animations drawn, and the last few errors. */
public final class Stats {

    private static final long STARTED = System.currentTimeMillis();
    private static final Map<String, LongAdder> COMMANDS = new ConcurrentHashMap<>();
    private static final LongAdder RENDERS = new LongAdder();
    private static final LongAdder RENDER_MS = new LongAdder();
    private static final AtomicLong RENDER_MAX = new AtomicLong();
    private static final LongAdder RENDER_FAILED = new LongAdder();
    private static final LongAdder RENDER_REJECTED = new LongAdder();
    private static final LongAdder REDUCED = new LongAdder();
    private static final Deque<String> ERRORS = new ArrayDeque<>();
    private static final int KEEP_ERRORS = 5;

    private Stats() { }

    public static void command(String key) {
        COMMANDS.computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    public static void render(long ms) {
        RENDERS.increment();
        RENDER_MS.add(ms);
        RENDER_MAX.accumulateAndGet(ms, Math::max);
    }

    public static void renderFailed() {
        RENDER_FAILED.increment();
    }

    public static void renderRejected() {
        RENDER_REJECTED.increment();
    }

    /** An animation skipped because the person turned animations off. */
    public static void reduced() {
        REDUCED.increment();
    }

    public static void error(String command, Throwable e) {
        String line = "<t:" + System.currentTimeMillis() / 1000 + ":R> `" + command + "` " + e.getClass().getSimpleName()
                + (e.getMessage() == null ? "" : ": " + (e.getMessage().length() > 80 ? e.getMessage().substring(0, 80) : e.getMessage()));
        synchronized (ERRORS) {
            ERRORS.addFirst(line);
            while (ERRORS.size() > KEEP_ERRORS) ERRORS.removeLast();
        }
    }

    /** A copy of the numbers at one moment. */
    public static final class Snapshot {
        public final long uptimeMs;
        public final long commandsRun;
        public final List<String> topCommands = new ArrayList<>();
        public final long renders;
        public final double renderAvgMs;
        public final long renderMaxMs;
        public final long renderFailed;
        public final long renderRejected;
        public final long reducedMotion;
        public final List<String> recentErrors;

        Snapshot(long uptimeMs, long commandsRun, long renders, double renderAvgMs, long renderMaxMs, long renderFailed,
                 long renderRejected, long reducedMotion, List<String> recentErrors) {
            this.uptimeMs = uptimeMs;
            this.commandsRun = commandsRun;
            this.renders = renders;
            this.renderAvgMs = renderAvgMs;
            this.renderMaxMs = renderMaxMs;
            this.renderFailed = renderFailed;
            this.renderRejected = renderRejected;
            this.reducedMotion = reducedMotion;
            this.recentErrors = recentErrors;
        }
    }

    public static Snapshot snapshot() {
        long total = 0;
        List<Map.Entry<String, LongAdder>> entries = new ArrayList<>(COMMANDS.entrySet());
        for (Map.Entry<String, LongAdder> e : entries) total += e.getValue().sum();
        entries.sort((a, b) -> Long.compare(b.getValue().sum(), a.getValue().sum()));
        long renders = RENDERS.sum();
        List<String> errors;
        synchronized (ERRORS) {
            errors = new ArrayList<>(ERRORS);
        }
        Snapshot s = new Snapshot(System.currentTimeMillis() - STARTED, total, renders,
                renders == 0 ? 0 : RENDER_MS.sum() / (double) renders, RENDER_MAX.get(), RENDER_FAILED.sum(),
                RENDER_REJECTED.sum(), REDUCED.sum(), errors);
        for (int i = 0; i < Math.min(5, entries.size()); i++) {
            s.topCommands.add(entries.get(i).getKey() + " ×" + entries.get(i).getValue().sum());
        }
        return s;
    }

    /** @return for example {@code 2d 3h 4m} */
    public static String duration(long ms) {
        long m = ms / 60_000;
        long d = m / (60 * 24);
        long h = m / 60 % 24;
        return (d > 0 ? d + "d " : "") + (d > 0 || h > 0 ? h + "h " : "") + m % 60 + "m";
    }
}

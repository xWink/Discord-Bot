package command.ui;

import economy.ProfilePrefs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The "reduce motion" setting. Someone who turns animations off gets every result as plain text instead of a GIF, in every
 * game and lookup. The command dispatcher notes who is running a command for the length of that command, so the code that
 * would draw a GIF only has to ask {@link #reduced()}.
 */
public final class Motion {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();
    private static final Map<Long, Boolean> CACHE = new ConcurrentHashMap<>();

    private static volatile Function<Long, Boolean> source = user -> {
        try {
            return ProfilePrefs.isReducedMotion(user);
        } catch (Exception e) {
            return false; // if the setting cannot be read, animate
        }
    };

    private Motion() { }

    /** Called by the dispatcher before a command runs. */
    public static void begin(long user) {
        CURRENT.set(user);
    }

    /** Called by the dispatcher when the command has returned. */
    public static void end() {
        CURRENT.remove();
    }

    /** @return true if whoever is running the current command wants no animations */
    public static boolean reduced() {
        Long user = CURRENT.get();
        return user != null && reduced(user);
    }

    public static boolean reduced(long user) {
        return CACHE.computeIfAbsent(user, u -> source.apply(u));
    }

    /** Saves the setting. */
    public static void set(long user, boolean reduce) throws java.sql.SQLException {
        ProfilePrefs.setReducedMotion(user, reduce);
        CACHE.put(user, reduce);
    }

    /** For tests: where the setting comes from, instead of the database. */
    public static void useSource(Function<Long, Boolean> replacement) {
        source = replacement;
        CACHE.clear();
    }
}

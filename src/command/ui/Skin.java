package command.ui;

import command.util.art.Theme;
import economy.ProfilePrefs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Which palette (dark or light) a person's pictures are drawn in. Works like {@link Motion}: the dispatcher notes who
 * is running a command, and the art code asks {@link Theme#current()}. Pictures drawn on a worker thread take the
 * palette along with the job (see {@link Reveal}).
 */
public final class Skin {

    private static final Map<Long, Theme> CACHE = new ConcurrentHashMap<>();

    private static volatile Function<Long, Theme> source = user -> {
        try {
            return ProfilePrefs.isLightTheme(user) ? Theme.LIGHT : Theme.DARK;
        } catch (Exception e) {
            return Theme.DARK; // if the setting cannot be read, use the default
        }
    };

    private Skin() { }

    /** Called by the dispatcher before a command runs. */
    public static void begin(long user) {
        Theme.use(of(user));
    }

    /** Called by the dispatcher when the command has returned. */
    public static void end() {
        Theme.clear();
    }

    public static Theme of(long user) {
        return CACHE.computeIfAbsent(user, u -> source.apply(u));
    }

    /** Saves the choice. */
    public static void set(long user, Theme theme) throws java.sql.SQLException {
        ProfilePrefs.setLightTheme(user, theme == Theme.LIGHT);
        CACHE.put(user, theme);
    }

    /** For tests: where the setting comes from, instead of the database. */
    public static void useSource(Function<Long, Theme> replacement) {
        source = replacement;
        CACHE.clear();
    }
}

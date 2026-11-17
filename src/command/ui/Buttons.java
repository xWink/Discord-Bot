package command.ui;

import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Buttons that run a text command when clicked. The button ID carries who may
 * press it, whether it disappears after one use, and the command to run:
 * {@code cmd:<user or 0>:<k|x>:<text>}.
 */
public final class Buttons {

    static final String PREFIX = "cmd:";

    private Buttons() { }

    /** A parsed button ID. */
    public static final class Parsed {
        private final long allowed;
        private final boolean consume;
        private final String text;

        public long allowed() { return allowed; }

        public boolean consume() { return consume; }

        public String text() { return text; }

        Parsed(long allowed, boolean consume, String text) {
            this.allowed = allowed;
            this.consume = consume;
            this.text = text;
        }
    }

    /**
     * @param allowed the only user who may click, or 0 for anyone
     * @param consume true to remove the buttons from the message once one is clicked
     * @param text the command to run, for example {@code !hit}
     * @param label the button text
     * @param style the button colour
     * @return a button
     */
    public static Button of(long allowed, boolean consume, String text, String label, ButtonStyle style) {
        String id = PREFIX + allowed + ":" + (consume ? "x" : "k") + ":" + text;
        if (id.length() > 100) throw new IllegalArgumentException("Button ID too long: " + id);
        return Button.of(style, id, label);
    }

    /** A one-use button only {@code owner} may press. */
    public static Button mine(long owner, String text, String label, ButtonStyle style) {
        return of(owner, true, text, label, style);
    }

    /**
     * Answer buttons A, B, C... for a multiple-choice question.
     *
     * @param allowed who may answer, or 0 for anyone
     * @param consume whether the buttons go away after the first click
     * @param command the answer command, for example {@code !answer}
     * @param count how many choices there are
     * @return the buttons, at most five
     */
    public static List<ItemComponent> answers(long allowed, boolean consume, String command, int count) {
        List<ItemComponent> out = new ArrayList<>();
        for (int i = 0; i < Math.min(count, 5); i++) {
            char letter = (char) ('A' + i);
            out.add(of(allowed, consume, command + " " + Character.toLowerCase(letter), String.valueOf(letter),
                    ButtonStyle.PRIMARY));
        }
        return out;
    }

    /** Hit and Stand for the player's blackjack hand. */
    public static List<ItemComponent> blackjack(long owner) {
        List<ItemComponent> out = new ArrayList<>();
        out.add(of(owner, true, "!hit", "Hit", ButtonStyle.PRIMARY));
        out.add(of(owner, true, "!stand", "Stand", ButtonStyle.SECONDARY));
        return out;
    }

    /** One button, in a list, for {@code setActionRow}. */
    public static List<ItemComponent> single(Button b) {
        List<ItemComponent> out = new ArrayList<>();
        out.add(b);
        return out;
    }

    /** @return the parsed ID, or null if it is not one of ours */
    public static Parsed parseId(String id) {
        if (id == null || !id.startsWith(PREFIX)) return null;
        String[] p = id.substring(PREFIX.length()).split(":", 3);
        if (p.length != 3) return null;
        try {
            return new Parsed(Long.parseLong(p[0]), p[1].equals("x"), p[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

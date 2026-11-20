package command.util.art;

import java.awt.Color;

/**
 * The two palettes the pictures can be drawn in. Both are neutral greys; the only colour is green and red where
 * something won, lost or is right. A person picks theirs with {@code !theme}; the command dispatcher notes who is
 * running a command and {@link #current()} gives the drawing code that person's palette.
 */
public enum Theme {

    DARK("dark",
            0x1F1F23, 0x141417, 0x3A3A41, 0x55555E, 0x26262B, 0x5C5C68, 0x34343B,
            0xF1F1F3, 0xA3A3AB, 0x76767F, 0x0A0A0C, 0x3FBF6B, 0xEF4B4B),
    LIGHT("light",
            0xE4E5E8, 0xCDCED3, 0xF6F6F8, 0xFFFFFF, 0xBDBEC4, 0xB0B1B9, 0xB4B5BB,
            0x17171B, 0x56575F, 0x80818A, 0x17171B, 0x1F8F45, 0xC9302C);

    private static final ThreadLocal<Theme> CURRENT = new ThreadLocal<>();

    public final String label;
    public final Color card;
    public final Color inset;
    public final Color face;
    public final Color faceHi;
    public final Color faceLo;
    public final Color select;
    public final Color divider;
    public final Color text;
    public final Color muted;
    public final Color faint;
    /** The hard border and shadow colour. */
    public final Color ink;
    public final Color ok;
    public final Color bad;

    Theme(String label, int card, int inset, int face, int faceHi, int faceLo, int select, int divider, int text,
          int muted, int faint, int ink, int ok, int bad) {
        this.label = label;
        this.card = new Color(card);
        this.inset = new Color(inset);
        this.face = new Color(face);
        this.faceHi = new Color(faceHi);
        this.faceLo = new Color(faceLo);
        this.select = new Color(select);
        this.divider = new Color(divider);
        this.text = new Color(text);
        this.muted = new Color(muted);
        this.faint = new Color(faint);
        this.ink = new Color(ink);
        this.ok = new Color(ok);
        this.bad = new Color(bad);
    }

    /** @return the palette for whoever the current thread is drawing for; dark if nobody is set */
    public static Theme current() {
        Theme t = CURRENT.get();
        return t == null ? DARK : t;
    }

    /** Draw in this palette on the calling thread until {@link #clear()}. */
    public static void use(Theme theme) {
        if (theme == null) CURRENT.remove();
        else CURRENT.set(theme);
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** @return the theme named {@code word} ("light" or "dark", any case), or null */
    public static Theme parse(String word) {
        for (Theme t : values()) if (t.label.equalsIgnoreCase(word.trim())) return t;
        return null;
    }
}

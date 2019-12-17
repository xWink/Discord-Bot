package command.util.cards;

import java.util.HashMap;
import java.util.Map;

public enum CardSuit {

    SPADES("spades", "\\♠", "s"),
    CLUBS("clubs", "\\♣", "c"),
    HEARTS("hearts", "\\♥", "h"),
    DIAMONDS("diamonds", "\\♦", "d");

    private static final Map<String, CardSuit> MY_MAP = new HashMap<>();

    static {
        for (CardSuit cardSuit : values()) {
            MY_MAP.put(cardSuit.getInitial(), cardSuit);
        }
    }

    private final String name;
    private final String emote;
    private final String initial;

    CardSuit(String theName, String theEmote, String theInitial) {
        name = theName;
        emote = theEmote;
        initial = theInitial;
    }

    /**
     * Name getter.
     *
     * @return the suit's name
     */
    public String getName() {
        return name;
    }

    /**
     * Emote getter.
     *
     * @return the emote that represents the suit on Discord
     */
    public String getEmote() {
        return emote;
    }

    /**
     * Initial getter.
     *
     * @return the initial letter of the suit's name
     */
    public String getInitial() {
        return initial;
    }

    /**
     * Getter for a new CardSuit based on initial.
     *
     * @param initial the initial letter of the suit
     * @return a CardSuit based on the initial letter argument
     */
    public static CardSuit getCardSuit(String initial) {
        return MY_MAP.get(initial);
    }
}

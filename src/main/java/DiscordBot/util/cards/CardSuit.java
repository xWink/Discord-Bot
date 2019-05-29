package DiscordBot.util.cards;

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

    CardSuit(String name, String emote, String initial){
        this.name = name;
        this.emote = emote;
        this.initial = initial;
    }

    public String getName(){
        return name;
    }

    public String getEmote(){
        return emote;
    }

    public String getInitial(){
        return initial;
    }

    public static CardSuit getByInitial(String initial){
        return MY_MAP.get(initial);
    }
}

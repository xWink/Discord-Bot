package DiscordBot.util.cards;

import java.util.HashMap;
import java.util.Map;

public enum CardRank {

    TWO("2 ", 2),
    THREE("3 ", 3),
    FOUR("4 ", 4),
    FIVE("5 ", 5),
    SIX("6 ", 6),
    SEVEN("7 ", 7),
    EIGHT("8 ", 8),
    NINE("9 ", 9),
    TEN("10 ", 10),
    JACK("J ", 10),
    QUEEN("Q ", 10),
    KING("K ", 10),
    ACE("A ", 11);

    private static final Map<String, CardRank> MY_MAP = new HashMap<>();
    static {
        for (CardRank cardRank : values()) {
            MY_MAP.put(cardRank.getSymbol(), cardRank);
        }
    }

    private String symbol;
    private int value;

    CardRank(String symbol, int value){
        this.symbol = symbol;
        this.value = value;
    }

    public String getSymbol(){
        return symbol;
    }

    public int getValue(){
        return value;
    }

    public static CardRank getBySymbol(String symbol){
        return MY_MAP.get(symbol);
    }
}

package command.util.cards;

import java.util.HashMap;
import java.util.Map;

public enum CardRank {

    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("10", 10),
    JACK("J", 10),
    QUEEN("Q", 10),
    KING("K", 10),
    ACE("A", 11);

    private static final Map<String, CardRank> MY_MAP = new HashMap<>();

    static {
        for (CardRank cardRank : values()) {
            MY_MAP.put(cardRank.getSymbol(), cardRank);
        }
    }

    private String symbol;
    private int value;

    CardRank(String theSymbol, int theValue) {
        symbol = theSymbol;
        value = theValue;
    }

    /**
     * Symbol getter.
     *
     * @return the rank's symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Value getter.
     *
     * @return the rank's numerical value
     */
    public int getValue() {
        return value;
    }

    /**
     * Getter for a new CardRank based on symbol.
     *
     * @param symbol the symbol for the rank
     * @return a CardRank based on the symbol argument
     */
    public static CardRank getCardRank(String symbol) {
        return MY_MAP.get(symbol);
    }
}

package command.util.cards;

import java.util.ArrayList;
import java.util.List;

/**
 * Text form for storing cards in the database, e.g. {@code "10 h,A s"}.
 */
public final class CardCodec {

    private CardCodec() { }

    public static String encode(List<Card> cards) {
        StringBuilder sb = new StringBuilder();
        for (Card card : cards) {
            if (sb.length() > 0) sb.append(',');
            sb.append(card.toDbFormat());
        }
        return sb.toString();
    }

    /**
     * @throws IllegalArgumentException if any card cannot be parsed
     */
    public static List<Card> decode(String text) {
        List<Card> cards = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return cards;
        for (String part : text.split(",")) {
            String[] bits = part.trim().split(" ");
            CardRank rank = bits.length == 2 ? CardRank.getCardRank(bits[0]) : null;
            CardSuit suit = bits.length == 2 ? CardSuit.getCardSuit(bits[1]) : null;
            if (rank == null || suit == null) throw new IllegalArgumentException("Bad card: " + part);
            cards.add(new Card(rank, suit));
        }
        return cards;
    }
}

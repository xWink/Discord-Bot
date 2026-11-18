package command.util.game;

import command.util.game.SlotsGame.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Scratch card. Nine hidden symbols; three of the same win that symbol's prize. The outcome is drawn first and the
 * card is laid out to match, so no card ever shows an accidental second win. About 92% is paid back.
 */
public final class ScratchGame {

    /** A prize, the symbol that wins it, and how likely it is (per 10,000 cards). */
    public enum Prize {
        X1_5(Symbol.CHERRY, 15, 1900), X3(Symbol.LEMON, 30, 850), X5(Symbol.BELL, 50, 350),
        X10(Symbol.STAR, 100, 120), X50(Symbol.MASCOT, 500, 16);

        public final Symbol symbol;
        public final int tenths;
        final int chance;

        Prize(Symbol symbol, int tenths, int chance) {
            this.symbol = symbol;
            this.tenths = tenths;
            this.chance = chance;
        }
    }

    /** A dealt card: the nine cells, and the prize if there is one. */
    public static final class Card {
        public final Symbol[] cells;
        public final Prize prize;

        Card(Symbol[] cells, Prize prize) {
            this.cells = cells;
            this.prize = prize;
        }
    }

    private ScratchGame() { }

    public static Card deal(Random random) {
        int roll = random.nextInt(10_000);
        Prize prize = null;
        for (Prize p : Prize.values()) {
            roll -= p.chance;
            if (roll < 0) {
                prize = p;
                break;
            }
        }
        List<Symbol> cells = new ArrayList<>();
        int[] count = new int[Symbol.values().length];
        if (prize != null) {
            for (int i = 0; i < 3; i++) cells.add(prize.symbol);
            count[prize.symbol.ordinal()] = 3;
        }
        while (cells.size() < 9) {
            Symbol s = Symbol.values()[random.nextInt(Symbol.values().length)];
            if (count[s.ordinal()] >= 2) continue; // never a second triple
            count[s.ordinal()]++;
            cells.add(s);
        }
        Collections.shuffle(cells, random);
        return new Card(cells.toArray(new Symbol[0]), prize);
    }

    /** @return the coins paid back for the stake, before taking the stake off */
    public static int payout(Card card, int stake) {
        return card.prize == null ? 0 : (int) ((long) stake * card.prize.tenths / 10);
    }

    public static int net(Card card, int stake) {
        return payout(card, stake) - stake;
    }
}

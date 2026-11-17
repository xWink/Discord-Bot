package command.util.game;

import command.util.cards.CardCodec;
import command.util.cards.DeckOfCards;
import command.util.cards.HandOfCards;
import database.Database;
import economy.EconomyService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists open blackjack hands so a restart cannot lose a stake.
 */
public final class BlackJackStore {

    private BlackJackStore() { }

    /**
     * Opens a hand: reserves the stake and stores the game in one transaction.
     * Fails (and reserves nothing) if the player already has an open hand.
     *
     * @throws EconomyService.InsufficientFundsRuntime if the stake is not spendable
     */
    public static void open(BlackJackGame game) throws SQLException {
        Database.inTransaction(c -> {
            EconomyService.reserveIn(c, game.getPlayer().getUserId(), game.getBet(), "blackjack",
                    game.getReservationId(), game.getReservationId());
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO blackjack_games (user, bet, reservation_id, "
                    + "player_hand, dealer_hand, deck, channel_id, last_action, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                bind(ps, game, true);
                ps.executeUpdate();
            }
            return null;
        });
    }

    /** Saves the current hands and deck after a move. */
    public static void save(BlackJackGame game) throws SQLException {
        Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE blackjack_games SET player_hand = ?, "
                    + "dealer_hand = ?, deck = ?, last_action = ? WHERE user = ?")) {
                ps.setString(1, CardCodec.encode(game.getPlayer().getHand().getAsList()));
                ps.setString(2, CardCodec.encode(game.getDealer().getHand().getAsList()));
                ps.setString(3, CardCodec.encode(game.getDeck().getCards()));
                ps.setLong(4, game.getLastAction());
                ps.setLong(5, game.getPlayer().getUserId());
                ps.executeUpdate();
            }
            return null;
        });
    }

    /**
     * Settles a round exactly once: pays out or collects the stake and removes
     * the stored game in the same transaction.
     *
     * @return the net change to the wallet, or 0 if it was already settled
     */
    public static int settle(BlackJackGame game) throws SQLException {
        synchronized (game) {
            if (game.isSettled()) return 0;
            int net = game.checkWinner();
            Database.inTransaction(c -> {
                EconomyService.settleReservationIn(c, game.getReservationId(), net, "blackjack");
                delete(c, game.getPlayer().getUserId());
                return null;
            });
            game.markSettled();
            BlackJackList.removeGame(game);
            return net;
        }
    }

    /**
     * Loads every stored hand at startup. A hand that cannot be restored is
     * refunded and removed.
     *
     * @return the games that were restored
     */
    public static List<BlackJackGame> recover() throws SQLException {
        List<BlackJackGame> restored = new ArrayList<>();
        List<long[]> broken = new ArrayList<>();
        List<String> brokenReservations = new ArrayList<>();
        Database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT user, bet, reservation_id, player_hand, "
                    + "dealer_hand, deck, channel_id FROM blackjack_games");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long user = rs.getLong(1);
                    try {
                        Player player = new Player(user, new HandOfCards(CardCodec.decode(rs.getString(4))));
                        Player dealer = new Player(0, new HandOfCards(CardCodec.decode(rs.getString(5))));
                        DeckOfCards deck = DeckOfCards.of(CardCodec.decode(rs.getString(6)));
                        BlackJackGame game = new BlackJackGame(player, dealer, rs.getInt(2), deck, rs.getString(3));
                        game.setChannelId(rs.getLong(7));
                        restored.add(game);
                    } catch (RuntimeException e) {
                        broken.add(new long[]{user});
                        brokenReservations.add(rs.getString(3));
                    }
                }
            }
            return null;
        });
        for (int i = 0; i < broken.size(); i++) {
            final String reservation = brokenReservations.get(i);
            final long user = broken.get(i)[0];
            Database.inTransaction(c -> {
                EconomyService.refundReservationIn(c, reservation);
                delete(c, user);
                return null;
            });
        }
        for (BlackJackGame game : restored) {
            BlackJackList.addGame(game);
        }
        return restored;
    }

    private static void delete(Connection c, long user) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM blackjack_games WHERE user = ?")) {
            ps.setLong(1, user);
            ps.executeUpdate();
        }
    }

    private static void bind(PreparedStatement ps, BlackJackGame game, boolean insert) throws SQLException {
        ps.setLong(1, game.getPlayer().getUserId());
        ps.setInt(2, game.getBet());
        ps.setString(3, game.getReservationId());
        ps.setString(4, CardCodec.encode(game.getPlayer().getHand().getAsList()));
        ps.setString(5, CardCodec.encode(game.getDealer().getHand().getAsList()));
        ps.setString(6, CardCodec.encode(game.getDeck().getCards()));
        ps.setLong(7, game.getChannelId());
        ps.setLong(8, game.getLastAction());
        ps.setLong(9, System.currentTimeMillis());
    }
}

package command.util.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BlackJackList {

    private BlackJackList() { }

    private static final List<BlackJackGame> games = Collections.synchronizedList(new ArrayList<>());

    /**
     * Adds a game to the list of active games.
     *
     * @param game the game being added to the list
     */
    public static void addGame(BlackJackGame game) {
        games.add(game);
    }

    /**
     * Snapshot of the active games, safe to iterate while others change.
     *
     * @return a copy of the list of active games
     */
    public static List<BlackJackGame> getGames() {
        synchronized (games) {
            return new ArrayList<>(games);
        }
    }

    /**
     * Removes a game from the list of active games.
     *
     * @param game the game being removed from the list
     */
    public static void removeGame(BlackJackGame game) {
        games.remove(game);
    }

    /**
     * Gets the active game for a user.
     *
     * @param userId the userId being searched for in the list of games
     * @return the user's game, or null if they have none
     */
    public static BlackJackGame getUserGame(long userId) {
        synchronized (games) {
            for (BlackJackGame game : games) {
                if (game.getPlayer().getUserId() == userId) {
                    return game;
                }
            }
        }
        return null;
    }
}

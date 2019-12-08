package command.util.game;

import java.util.ArrayList;

public final class BlackJackList {

    private BlackJackList() { }

    private static ArrayList<BlackJackGame> games;

    static {
        games = new ArrayList<>();
    }

    /**
     * Adds a game to the list of active games.
     *
     * @param game the game being added to the list
     */
    public static void addGame(BlackJackGame game) {
        games.add(game);
    }

    /**
     * Getter for the list of active games.
     *
     * @return the list of active games
     */
    public static ArrayList<BlackJackGame> getGames() {
        return games;
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
     * Gets a game from the list of active games where the player
     * has the same userId as the parameter.
     *
     * @param userId the userId being searched for in the list of games
     * @return true if a game with a player with a matching userId is found
     */
    public static BlackJackGame getUserGame(long userId) {
        for (BlackJackGame game : games) {
            if (game.getPlayer().getUserId() == userId) {
                return game;
            }
        }
        return null;
    }
}

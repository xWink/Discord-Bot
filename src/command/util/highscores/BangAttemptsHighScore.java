package command.util.highscores;


import java.util.ArrayList;

public class BangAttemptsHighScore extends BangHighScore {

    BangAttemptsHighScore() {
        super("Most bang attempts");
    }

    /**
     * Updates the high score list for top 10 most bang attempts.
     * @see BangHighScore
     * @see BangPlayer
     */
    @Override
    public void setScores() {
        try {
            ArrayList<Player> players = bc.getMostAttemptsPlayers();
            try {
                players.subList(10, players.size()).clear();
            } catch (Exception ignored) { }
            setPlayers(players);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

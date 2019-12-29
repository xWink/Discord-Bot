package command.util.highscores;

import java.util.ArrayList;

public final class HighScoreList {

    private static ArrayList<HighScore> highScores;

    private HighScoreList() { }

    static {
        highScores = new ArrayList<>();
        highScores.add(BangHighScore.getBangHighScore());
    }

    /**
     * Updates all high scores in the list.
     */
    public static void update() {
        for (HighScore highScore : highScores) {
            highScore.update();
        }
    }
}

package command.util.highscores;

import command.util.highscores.bang.BangHighScore;

import java.util.ArrayList;

public class HighScoreList {

    private static ArrayList<HighScore> highScores;

    private HighScoreList() { }

    static {
        highScores = new ArrayList<>();
        highScores.add(new BangHighScore());
    }

    public static void update() {
        for (HighScore highScore : highScores) {
            highScore.update();
        }
    }
}

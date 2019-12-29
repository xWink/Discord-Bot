package main.timertasks;

import command.util.highscores.HighScoreList;

import java.util.TimerTask;

public class UpdateHighScores extends TimerTask {

    /**
     * Empty constructor.
     */
    public UpdateHighScores() { }

    /**
     * Updates HighScoreList.
     *
     * @see HighScoreList
     */
    @Override
    public void run() {
        HighScoreList.update();
    }
}

package main.timertasks;

import command.util.highscores.HighScoreList;

import java.util.TimerTask;

public class UpdateHighScores extends TimerTask {

    public UpdateHighScores() { }

    public void run() {
        HighScoreList.update();
    }
}

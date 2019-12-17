package command.util.highscores;

import database.connectors.BangConnector;


public abstract class BangHighScore extends HighScore {

    protected BangConnector bc;

    BangHighScore(String highScoreType) {
        super(highScoreType);
        bc = new BangConnector();
    }

    public abstract void setScores();
}

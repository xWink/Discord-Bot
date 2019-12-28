package command.util.highscores.bang;

import command.util.highscores.Player;

public class BangPlayer extends Player {

    private int attempts;
    private int deaths;
    private int jams;

    public BangPlayer(long userId, int numTries, int numDeaths, int numJams) {
        super(userId);
        attempts = numTries;
        deaths = numDeaths;
        jams = numJams;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getJams() {
        return jams;
    }

    public double getSurvivalRate() {
        return 100 - (Math.round((double) (deaths / attempts) * 10d) / 10d);
    }
}

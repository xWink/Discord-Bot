package command.util.highscores;

public class BangPlayer extends Player {

    private int tries;
    private int deaths;
    private int jams;

    public BangPlayer(long userId, int numTries, int numDeaths, int numJams) {
        super(userId);
        tries = numTries;
        deaths = numDeaths;
        jams = numJams;
    }

    public int getTries() {
        return tries;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getJams() {
        return jams;
    }

    public double getSurvivalRate() {
        return 100 - (Math.round((double) (deaths / tries) * 10d) / 10d);
    }
}

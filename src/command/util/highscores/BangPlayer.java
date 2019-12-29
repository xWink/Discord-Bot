package command.util.highscores;

public class BangPlayer extends Player {

    private int attempts;
    private int deaths;
    private int jams;

    /**
     * Initializes user ID, number of attempts, tries, deaths, and jams for a
     * player of Bang.
     *
     * @param userId the ID number of the user on Discord
     * @param numTries the number of times the user played Bang
     * @param numDeaths the number of times the user died in Bang
     * @param numJams the number of times the user got a jam in Bang
     */
    public BangPlayer(long userId, int numTries, int numDeaths, int numJams) {
        super(userId);
        attempts = numTries;
        deaths = numDeaths;
        jams = numJams;
    }

    /**
     * Returns the number of times the user
     * played Bang.
     *
     * @return number of bang attempts
     */
    public int getAttempts() {
        return attempts;
    }

    /**
     * Returns the number of times the user
     * died in Bang.
     *
     * @return number of bang deaths
     */
    public int getDeaths() {
        return deaths;
    }

    /**
     * Returns the number of times the user
     * jammed the gun in Bang.
     *
     * @return number of bang jams
     */
    public int getJams() {
        return jams;
    }

    /**
     * Returns the Bang survival rate of the user.
     *
     * @return Percentage survival rate in Bang
     */
    public double getSurvivalRate() {
        return 100 - (Math.round((double) (deaths / attempts) * 10d) / 10d);
    }
}

package command.util.cache;

public class BangUpdate {

    private long id;
    private int attempts;
    private int deaths;
    private int jams;
    private boolean rewarded;

    /**
     * Creates a new BangUpdate object, which contains the data
     * that is used for an update to the bang table.
     * Initializes userId to parameter. Initializes attempts,
     * deaths, and jams to 0 and rewarded to false.
     *
     * @param userId the ID number of the user whose bang row is being updated
     */
    public BangUpdate(long userId) {
        id = userId;
        attempts = 0;
        deaths = 0;
        jams = 0;
        rewarded = false;
    }

    /**
     * Adds a specified number of attempts to the update.
     *
     * @param numAttempts the number of attempts to add
     */
    public void addAttempts(int numAttempts) {
        attempts += numAttempts;
    }

    /**
     * Adds a specified number of deaths to the update.
     *
     * @param numDeaths the number of deaths to add
     */
    public void addDeaths(int numDeaths) {
        deaths += numDeaths;
    }

    /**
     * Adds a specified number of jams to the update.
     *
     * @param numJams the number of jams to add
     */
    public void addJams(int numJams) {
        jams += numJams;
    }

    /**
     * Sets whether a reward is being given or not from a bang game.
     *
     * @param toReward whether a reward is being given or not
     */
    public void setReward(boolean toReward) {
        rewarded = toReward;
    }

    /**
     * Attempts getter.
     *
     * @return number of attempts in the update
     */
    public int getAttempts() {
        return attempts;
    }

    /**
     * Deaths getter.
     *
     * @return number of deaths in the update
     */
    public int getDeaths() {
        return deaths;
    }

    /**
     * Jams getter.
     *
     * @return number of jams in the update
     */
    public int getJams() {
        return jams;
    }

    /**
     * Rewarded getter.
     *
     * @return whether a reward is given in this update or not
     */
    public boolean isRewarded() {
        return rewarded;
    }

    /**
     * Id getter.
     *
     * @return the ID number of the user whose bang row is being updated
     */
    public long getId() {
        return id;
    }
}

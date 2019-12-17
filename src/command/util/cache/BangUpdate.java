package command.util.cache;


import java.util.Date;

public class BangUpdate {

    private long id;
    private long lastPlayed;
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
        this(userId, new Date().getTime(), 0, 0, 0, false);
    }

    /**
     * Creates a new BangUpdate object, which contains the data
     * that is used for an update to the bang table.
     * Initializes instance variables to parameters.
     *
     * @param userId the ID number of the user whose bang row is being updated
     * @param numAttempts the number of bang attempts to add in the update
     * @param lastTimePlayed the last time the user played
     * @param numDeaths the number od deaths to add in the update
     * @param numJams the number of jams to add in the update
     * @param getsReward whether the user gets a daily reward in the update
     */
    public BangUpdate(long userId, long lastTimePlayed, int numAttempts, int numDeaths, int numJams, boolean getsReward) {
        id = userId;
        lastPlayed = lastTimePlayed;
        attempts = numAttempts;
        deaths = numDeaths;
        jams = numJams;
        rewarded = getsReward;
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
     * Sets the time the user last played bang.
     *
     * @param lastTimePlayed the number of ms since epoch that
     * represents the last time the user played bang
     */
    public void setLastPlayed(long lastTimePlayed) {
        lastPlayed = lastTimePlayed;
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
     * Last time played getter.
     *
     * @return the number of ms since epoch that
     * represents the last time the user played bang
     */
    public long getLastPlayed() {
        return lastPlayed;
    }

    /**
     * Id getter.
     *
     * @return the ID number of the user whose bang row is being updated
     */
    public long getId() {
        return id;
    }

    /**
     * Creates an SQL update for the bang table based on the data
     * in this object.
     *
     * @return a string that is an SQL update for the bang table
     */
    public String toSQL() {
        return "UPDATE bang SET "
                + "tries = tries + " + getAttempts()
                + ", deaths = deaths + " + getDeaths()
                + ", jams = jams + " + getJams()
                + ", last_played = " + getLastPlayed()
                + (isRewarded() ? ", last_daily = " + getLastPlayed() : "")
                + " WHERE user = " + getId();
    }
}

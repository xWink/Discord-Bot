package DiscordBot.util.bang_util;

import net.dv8tion.jda.core.entities.User;

public class BangHighScores {

    private double attemptCount;
    private User mostAttemptsPlayer;
    private double bestRate;
    private User luckiest;
    private double worstRate;
    private User unluckiest;
    private int wealth;
    private User wealthiest;

    BangHighScores(
            double attemptCount,
            User mostAttemptsPlayer,
            double bestRate,
            User luckiest,
            double worstRate,
            User unluckiest,
            int wealth,
            User wealthiest){

        this.attemptCount = attemptCount;
        this.mostAttemptsPlayer = mostAttemptsPlayer;
        this.bestRate = bestRate;
        this.luckiest = luckiest;
        this.worstRate = worstRate;
        this.unluckiest = unluckiest;
        this.wealth = wealth;
        this.wealthiest = wealthiest;
    }

    public double getAttemptCount(){
        return attemptCount;
    }

    public User getMostAttemptsPlayer() {
        return mostAttemptsPlayer;
    }

    public double getBestRate() {
        return bestRate;
    }

    public User getLuckiest() {
        return luckiest;
    }

    public double getWorstRate() {
        return worstRate;
    }

    public User getUnluckiest() {
        return unluckiest;
    }

    public int getWealth() {
        return wealth;
    }

    public User getWealthiest() {
        return wealthiest;
    }
}

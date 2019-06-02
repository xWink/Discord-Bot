package DiscordBot.util.bang_util;

import net.dv8tion.jda.core.entities.User;

public class BangHighScores {

    private double attemptCount;
    private User mostAttemptsPlayer;
    private double deathCount;
    private User mostDeathsPlayer;
    private double bestRate;
    private User luckiest;
    private double worstRate;
    private User unluckiest;
    private int jamCount;
    private User mostJamsPlayer;

    BangHighScores(
            double attemptCount,
            User mostAttemptsPlayer,
            double deathCount,
            User mostDeathsPlayer,
            double bestRate,
            User luckiest,
            double worstRate,
            User unluckiest,
            int jamCount,
            User mostJamsPlayer){

        this.attemptCount = attemptCount;
        this.mostAttemptsPlayer = mostAttemptsPlayer;
        this.deathCount = deathCount;
        this.mostDeathsPlayer = mostDeathsPlayer;
        this.bestRate = bestRate;
        this.luckiest = luckiest;
        this.worstRate = worstRate;
        this.unluckiest = unluckiest;
        this.jamCount = jamCount;
        this.mostJamsPlayer = mostJamsPlayer;
    }

    public double getAttemptCount(){
        return attemptCount;
    }

    public User getMostAttemptsPlayer() {
        return mostAttemptsPlayer;
    }

    public double getDeathCount() {
        return deathCount;
    }

    public User getMostDeathsPlayer() {
        return mostDeathsPlayer;
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

    public int getJamCount() {
        return jamCount;
    }

    public User getMostJamsPlayer() {
        return mostJamsPlayer;
    }
}

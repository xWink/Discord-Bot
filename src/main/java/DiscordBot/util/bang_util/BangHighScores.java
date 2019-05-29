package DiscordBot.util.bang_util;

public class BangHighScores {

    private double attemptCount;
    private String mostAttemptsPlayer;
    private double deathCount;
    private String mostDeathsPlayer;
    private double bestRate;
    private String luckiest;
    private double worstRate;
    private String unluckiest;
    private int jamCount;
    private String mostJamsPlayer;

    BangHighScores(
            double attemptCount,
            String mostAttemptsPlayer,
            double deathCount,
            String mostDeathsPlayer,
            double bestRate,
            String luckiest,
            double worstRate,
            String unluckiest,
            int jamCount,
            String mostJamsPlayer){

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

    public String getMostAttemptsPlayer() {
        return mostAttemptsPlayer;
    }

    public double getDeathCount() {
        return deathCount;
    }

    public String getMostDeathsPlayer() {
        return mostDeathsPlayer;
    }

    public double getBestRate() {
        return bestRate;
    }

    public String getLuckiest() {
        return luckiest;
    }

    public double getWorstRate() {
        return worstRate;
    }

    public String getUnluckiest() {
        return unluckiest;
    }

    public int getJamCount() {
        return jamCount;
    }

    public String getMostJamsPlayer() {
        return mostJamsPlayer;
    }
}

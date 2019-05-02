package src.main.java.DiscordBot.helpers;

public class BangHighScores {

    public double attemptCount;
    public String mostAttemptsPlayer;
    public double deathCount;
    public String mostDeathsPlayer;
    public double bestRate;
    public String luckiest;
    public double worstRate;
    public String unluckiest;

    public BangHighScores(int attemptCount,
            String mostAttemptsPlayer,
            int deathCount,
            String mostDeathsPlayer,
            double bestRate,
            String luckiest,
            double worstRate,
            String unluckiest){

        this.attemptCount = attemptCount;
        this.mostAttemptsPlayer = mostAttemptsPlayer;
        this.deathCount = deathCount;
        this.mostDeathsPlayer = mostDeathsPlayer;
        this.bestRate = bestRate;
        this.luckiest = luckiest;
        this.worstRate = worstRate;
        this.unluckiest = unluckiest;
    }
}

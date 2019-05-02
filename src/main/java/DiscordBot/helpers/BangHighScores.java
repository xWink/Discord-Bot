package src.main.java.DiscordBot.helpers;

public class BangHighScores {

    int attemptCount;
    String mostAttemptsPlayer;
    int deathCount;
    String mostDeathsPlayer;
    double bestRate;
    String luckiest;
    double worstRate;
    String unluckiest;

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

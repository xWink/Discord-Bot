package DiscordBot.commands.Bang;

class BangHighScores {

    double attemptCount;
    String mostAttemptsPlayer;
    double deathCount;
    String mostDeathsPlayer;
    double bestRate;
    String luckiest;
    double worstRate;
    String unluckiest;
    int jamCount;
    String mostJamsPlayer;

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
}

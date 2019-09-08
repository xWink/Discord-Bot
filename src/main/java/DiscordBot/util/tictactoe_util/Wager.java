package DiscordBot.util.tictactoe_util;

public class Wager {

    private long challengerId, targetId;
    private int wagerAmount;

    public Wager(long challengerId, long targetId, int wagerAmount){
        this.challengerId = challengerId;
        this.targetId = targetId;
        this.wagerAmount = wagerAmount;
    }

    public long getChallengerId() {
        return challengerId;
    }

    public long getTargetId() {
        return targetId;
    }

    public int getWagerAmount() {
        return wagerAmount;
    }
}

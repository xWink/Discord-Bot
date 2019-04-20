package DiscordBot.commands;

public class BangPlayer {

    String player;
    double attempts;
    double deaths;

    public BangPlayer(String player, double attempts, double deaths){
        this.player = player;
        this.attempts = attempts;
        this.deaths = deaths;
    }
}

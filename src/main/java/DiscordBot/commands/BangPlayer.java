package DiscordBot.commands;

public class BangPlayer {

    String player;
    int attempts;
    int deaths;

    public BangPlayer(String player, int attempts, int deaths){
        this.player = player;
        this.attempts = attempts;
        this.deaths = deaths;
    }
}

package DiscordBot.commands.BlackJack;

public enum CardSuit {

    SPADES("spades", ":spades:", 's'),
    CLUBS("clubs", ":clubs:", 'c'),
    HEARTS("hearts", ":hearts:", 'h'),
    DIAMONDS("diamonds", ":diamonds:", 'd');

    private final String name;
    private final String emote;
    private final char initial;

    CardSuit(String name, String emote, char initial){
        this.name = name;
        this.emote = emote;
        this.initial = initial;
    }

    public String getName(){
        return name;
    }

    public String getEmote(){
        return emote;
    }

    public char getInitial(){
        return initial;
    }
}

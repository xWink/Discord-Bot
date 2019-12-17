package command.util.highscores;

import java.util.ArrayList;

public abstract class HighScore {

    private String type;
    private ArrayList<Player> players;

    HighScore(String highScoreType) {
        type = highScoreType;
        players = new ArrayList<>();
    }

    public String getType() {
        return type;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    protected void setPlayers(ArrayList<Player> thePlayers) {
        players = thePlayers;
    }
}

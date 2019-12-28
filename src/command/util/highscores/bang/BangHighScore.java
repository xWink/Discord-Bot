package command.util.highscores.bang;

import command.util.highscores.HighScore;
import database.connectors.BangConnector;

import java.util.ArrayList;


public class BangHighScore extends HighScore {

    private BangConnector bc;
    private ArrayList<BangPlayer> mostAttempts;
    private ArrayList<BangPlayer> luckiest;
    private ArrayList<BangPlayer> unluckiest;

    public BangHighScore() {
        bc = new BangConnector();
        update();
    }

    public void update() {
        updateMostAttempts();
    }

    private void updateMostAttempts() {
        mostAttempts = new ArrayList<>();
        try {
            mostAttempts = bc.getMostAttemptsPlayers();
            mostAttempts.subList(10, mostAttempts.size()).clear();
            //todo: update roles
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("**Bang high scores**:\n");
        appendMostAttempts(string);
        return string.toString();
    }

    private void appendMostAttempts(StringBuilder string) {
        string.append("Most attempts: ").append(mostAttempts.get(0).getAttempts()).append(" by ");

        for (BangPlayer player : mostAttempts) {
            if (player.getAttempts() == mostAttempts.get(0).getAttempts()) {
                if (!player.equals(mostAttempts.get(0)))
                    string.append(", ");
                string.append(guild.getMemberById(player.getId()).getEffectiveName());
            }
        }
    }
}

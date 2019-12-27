package command.util.highscores;

import database.connectors.BangConnector;

import java.util.ArrayList;


public class BangHighScore extends HighScore {

    private BangConnector bc;
    private ArrayList<BangPlayer> mostAttempts;
    private ArrayList<BangPlayer> luckiest;
    private ArrayList<BangPlayer> unluckiest;

    public BangHighScore() {
        bc = new BangConnector();
        updateMostAttempts();
    }

    private void updateMostAttempts() {
        mostAttempts = new ArrayList<>();
        try {
            mostAttempts = bc.getMostAttemptsPlayers();
            mostAttempts.subList(10, mostAttempts.size()).clear();
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
        string.append("Most attempts: ").append(mostAttempts.get(0).getTries()).append(" by ");

        for (BangPlayer player : mostAttempts) {
            if (player.getTries() == mostAttempts.get(0).getTries()) {
                if (!player.equals(mostAttempts.get(0)))
                    string.append(", ");
                string.append(guild.getMemberById(player.getId()).getEffectiveName());
            }
        }
    }
}

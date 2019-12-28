package command.util.highscores.bang;

import command.util.highscores.HighScore;
import database.connectors.BangConnector;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

import java.util.ArrayList;


public class BangHighScore extends HighScore {

    private static BangHighScore bangHighScore;

    private BangConnector bc;
    private ArrayList<BangPlayer> mostAttempts;
    private ArrayList<BangPlayer> luckiest;
    private ArrayList<BangPlayer> unluckiest;

    /**
     * Initializes the connection to the database and
     * updates the lists of top 10 players for most attempts,
     * luckiest, and unluckiest.
     */
    private BangHighScore() {
        bc = new BangConnector();
        update();
    }

    /**
     * Singleton instance getter.
     *
     * @return the BangHighScore instance
     */
    public static BangHighScore getBangHighScore() {
        if (bangHighScore == null)
            bangHighScore = new BangHighScore();

        return bangHighScore;
    }

    /**
     * Updates all of the high scores based on database values.
     */
    public void update() {
        updateMostAttempts();
    }

    /**
     * Updates the list of top 10 attempts players, then updates roles
     * of members in the server so only those tied for first place have
     * the specified role.
     */
    private void updateMostAttempts() {
        mostAttempts = new ArrayList<>();
        try {
            mostAttempts = bc.getMostAttemptsPlayers();
            mostAttempts.subList(10, mostAttempts.size()).clear();
            Role mostAttemptsRole = guild.getRoleById("573398286044626945");

            for (BangPlayer player : mostAttempts) {
                Member member = guild.getMemberById(player.getId());
                if (player.getAttempts() == mostAttempts.get(0).getAttempts()) {
                    guild.getController().addRolesToMember(member, mostAttemptsRole).queue();
                } else {
                    guild.getController().removeRolesFromMember(member, mostAttemptsRole).queue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a neatly formatted string containing data on all the high scores for bang.
     *
     * @return the neatly formatted string containing data on all the high scores for bang
     */
    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("**Bang high scores**:\n");
        appendMostAttempts(string);
        return string.toString();
    }

    /**
     * Appends a neatly formatted string based on the players with the most bang attempts
     * to a StringBuilder.
     *
     * @param string the StringBuilder which will have information appended to it
     */
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

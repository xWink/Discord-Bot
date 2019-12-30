package command.util.highscores;

import database.connectors.BangConnector;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

import java.util.ArrayList;


public final class BangHighScore extends HighScore {

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
            Role mostAttemptsRole = getGuild().getRoleById("573398286044626945");

            for (BangPlayer player : mostAttempts) {
                Member member = getGuild().getMemberById(player.getId());
                if (player.getAttempts() == mostAttempts.get(0).getAttempts()) {
                    getGuild().getController().addRolesToMember(member, mostAttemptsRole).queue();
                } else {
                    getGuild().getController().removeRolesFromMember(member, mostAttemptsRole).queue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the list of top 10 luckiest players, then updates roles
     * of members in the server so only those tied for first place have
     * the specified role.
     */
    private void updateLuckiest() {
        luckiest = new ArrayList<>();
        try {
            luckiest = bc.getLuckiestPlayers();
            luckiest.subList(10, luckiest.size()).clear();
            Role luckiestRole = getGuild().getRoleById("573398281108062208");

            for (BangPlayer player : luckiest) {
                Member member = getGuild().getMemberById(player.getId());
                if (player.getAttempts() == luckiest.get(0).getSurvivalRate()) {
                    getGuild().getController().addRolesToMember(member, luckiestRole).queue();
                } else {
                    getGuild().getController().removeRolesFromMember(member, luckiestRole).queue();
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
        StringBuilder string = new StringBuilder("**Bang High Scores**:\n");
        appendMostAttempts(string);
        appendLuckiest(string);
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
                string.append(getGuild().getMemberById(player.getId()).getEffectiveName());
            }
        }
    }

    /**
     * Appends a neatly formatted string based on the players with highest bang
     * survival rates to a StringBuilder.
     *
     * @param string the StringBuilder which will have information appended to it
     */
    private void appendLuckiest(StringBuilder string) {
        string.append("Highest survival rate: ").append(luckiest.get(0).getSurvivalRate()).append(" by ");

        for (BangPlayer player : luckiest) {
            if (player.getAttempts() == luckiest.get(0).getSurvivalRate()) {
                if (!player.equals(luckiest.get(0)))
                    string.append(", ");
                string.append(getGuild().getMemberById(player.getId()).getEffectiveName());
            }
        }
    }
}

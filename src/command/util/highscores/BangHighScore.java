package command.util.highscores;

import database.connectors.BangConnector;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

import java.util.ArrayList;
import java.util.Collections;


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
        updateLuckiest();
        updateUnluckiest();
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

            if (mostAttempts.size() > 10)
                mostAttempts.subList(10, mostAttempts.size()).clear();

            Role mostAttemptsRole = getGuild().getRoleById("573398286044626945");

            for (BangPlayer player : mostAttempts) {
                Member member = getGuild().getMemberById(player.getId());
                if (player.getAttempts() == mostAttempts.get(0).getAttempts())
                    getGuild().getController().addRolesToMember(member, mostAttemptsRole).queue();
                else
                    getGuild().getController().removeRolesFromMember(member, mostAttemptsRole).queue();
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

            if (luckiest.size() > 10)
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
     * Updates the list of top 10 unluckiest players, then updates roles
     * of members in the server so only those tied for first place have
     * the specified role.
     */
    private void updateUnluckiest() {
        unluckiest = new ArrayList<>();
        try {
            unluckiest = bc.getLuckiestPlayers();
            Collections.reverse(unluckiest);

            if (unluckiest.size() > 10)
                unluckiest.subList(10, unluckiest.size()).clear();

            Role unluckiestRole = getGuild().getRoleById("573398274598502410");

            for (BangPlayer player : unluckiest) {
                Member member = getGuild().getMemberById(player.getId());
                if (player.getAttempts() == unluckiest.get(0).getSurvivalRate()) {
                    getGuild().getController().addRolesToMember(member, unluckiestRole).queue();
                } else {
                    getGuild().getController().removeRolesFromMember(member, unluckiestRole).queue();
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
        return "**Bang High Scores**:\n"
                + getMostAttemptsString() + "\n"
                + getLuckiestString() + "\n"
                + getUnluckiestString() + "\n\n"
                + getTotalsString();
    }

    /**
     * Returns a string containing data based on the
     * players with the most bang attempts.
     *
     * @return a neatly formatted string containing the
     * top bang attempts players
     */
    private String getMostAttemptsString() {
        String string = "Most attempts: " + mostAttempts.get(0).getAttempts() + " by ";

        for (BangPlayer player : mostAttempts) {
            if (player.getAttempts() == mostAttempts.get(0).getAttempts()) {
                if (!player.equals(mostAttempts.get(0)))
                    string = string.concat(", ");
                string = string.concat(getGuild().getMemberById(player.getId()).getEffectiveName());
            }
        }

        return string;
    }

    /**
     * Returns a string containing data based on the
     * players with the highest survival rates in bang.
     *
     * @return a neatly formatted string containing the
     * top bang survival rate players
     */
    private String getLuckiestString() {
        String string = "Highest survival rate: " + luckiest.get(0).getSurvivalRate() + "% by ";

        for (BangPlayer player : luckiest) {
            if (player.getSurvivalRate() == luckiest.get(0).getSurvivalRate()) {
                if (!player.equals(luckiest.get(0)))
                    string = string.concat(", ");
                string = string.concat(getGuild().getMemberById(player.getId()).getEffectiveName());
            }
        }

        return string;
    }

    /**
     * Returns a string containing data based on the
     * players with the highest survival rates in bang.
     *
     * @return a neatly formatted string containing the
     * top bang survival rate players
     */
    private String getUnluckiestString() {
        String string = "Lowest survival rate: " + unluckiest.get(0).getSurvivalRate() + "% by ";

        for (BangPlayer player : unluckiest) {
            if (player.getSurvivalRate() == unluckiest.get(0).getSurvivalRate()) {
                if (!player.equals(unluckiest.get(0)))
                    string = string.concat(", ");
                string = string.concat(getGuild().getMemberById(player.getId()).getEffectiveName());
            }
        }

        return string;
    }

    private String getTotalsString() {
        String string = "";
        try {
            string = "Total attempts: " + bc.getTotalAttempts()
                    + "\nTotal deaths: " + bc.getTotalDeaths();
        } catch (Exception ignored) { }
        return string;
    }
}

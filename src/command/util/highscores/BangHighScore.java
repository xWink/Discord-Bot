package command.util.highscores;

import database.connectors.BangConnector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.awt.*;
import java.sql.SQLException;
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
            if (mostAttemptsRole != null) {
                for (BangPlayer player : mostAttempts) {
                    Member member = getGuild().getMemberById(player.getId());
                    if (member != null) {
                        if (player.getAttempts() == mostAttempts.get(0).getAttempts()) {
                            getGuild().addRoleToMember(member, mostAttemptsRole).queue();
                        } else {
                            getGuild().removeRoleFromMember(member, mostAttemptsRole).queue();
                        }
                    }
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

            if (luckiest.size() > 10)
                luckiest.subList(10, luckiest.size()).clear();

            Role luckiestRole = getGuild().getRoleById("573398281108062208");
            if (luckiestRole != null) {
                for (BangPlayer player : luckiest) {
                    Member member = getGuild().getMemberById(player.getId());
                    if (member != null) {
                        if (player.getSurvivalRate() == luckiest.get(0).getSurvivalRate()) {
                            getGuild().addRoleToMember(member, luckiestRole).queue();
                        } else {
                            getGuild().removeRoleFromMember(member, luckiestRole).queue();
                        }
                    }
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
            if (unluckiestRole != null) {
                for (BangPlayer player : unluckiest) {
                    Member member = getGuild().getMemberById(player.getId());
                    if (member != null) {
                        if (player.getSurvivalRate() == unluckiest.get(0).getSurvivalRate()) {
                            getGuild().addRoleToMember(member, unluckiestRole).queue();
                        } else {
                            getGuild().removeRoleFromMember(member, unluckiestRole).queue();
                        }
                    }
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
                Member member = getGuild().getMemberById(player.getId());
                if (member != null)
                    string = string.concat(member.getEffectiveName());
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
                Member member = getGuild().getMemberById(player.getId());
                if (member != null)
                    string = string.concat(member.getEffectiveName());
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
                Member member = getGuild().getMemberById(player.getId());
                if (member != null)
                    string = string.concat(member.getEffectiveName());
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

    /**
     * Returns an Embed version of the toString function.
     *
     * @return the Embed version of the toString function.
     */
    public EmbedBuilder toEmbed() throws SQLException {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.YELLOW);
        eb.setTitle("Bang High Scores");

        eb.addField("Most Attempts",            getMostAttemptsString().replace("Most attempts: ", ""), false);
        eb.addField("Highest Survival Rate",    getLuckiestString().replace("Highest survival rate: ", ""), false);
        eb.addField("Lowest Survival Rate",     getUnluckiestString().replace("Lowest survival rate: ", ""), false);
        eb.addField("Total Attempts",           "" + bc.getTotalAttempts(), true);
        eb.addField("Total Deaths",             "" + bc.getTotalDeaths(), true);

        return eb;
    }
}

package command.commands.misc;

import command.Command;
import database.connectors.BangConnector;
import database.connectors.EconomyConnector;
import database.connectors.KarmaConnector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Profile extends Command {

    private MessageReceivedEvent event;
    private EmbedBuilder eb;
    private BangConnector bc;
    private EconomyConnector ec;
    private KarmaConnector kc;

    private int attempts = 0, deaths = 0, jams = 0, streak = 0, karma = 0, wallet = 0;
    private double survivalRate = 0;

    /**
     * Initializes the command's key to "!profile".
     */
    public Profile() {
        super("!profile", true);
        bc = new BangConnector();
        ec = new EconomyConnector();
        kc = new KarmaConnector();
        eb = new EmbedBuilder();
    }

    @Override
    public void start(MessageReceivedEvent event) {
        this.event = event;
        setBangProfile();
        event.getChannel().sendMessage(eb.build()).queue();
    }

    private void setBangProfile() {
        try {
            setBangData();
            setKarmaData();
            setWalletData();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        } finally {
            if (event.getMember() != null) {
                eb.setColor(event.getMember().getColor());
            } else {
                eb.setColor(Color.LIGHT_GRAY);
            }
            buildEB();
        }
    }

    private void buildEB() {
        eb.setTitle(event.getAuthor().getName() + "'s Profile");
        eb.setThumbnail(event.getAuthor().getAvatarUrl());

        eb.addField("Attempts", "" + attempts, true);
        eb.addField("Deaths", "" + deaths, true);
        eb.addField("Jams", "" + jams, true);
        eb.addField("Survival Rate", survivalRate + "%", true);
        eb.addField("Streak", streak + (streak > 0 ? " :fire:" : ""), true);
        eb.addField("Karma", "" + karma, false);
        eb.addField("Wallet", wallet + " *gc*", false);
    }

    private void setBangData() throws SQLException {
        ResultSet rs = bc.getUserRow(event.getAuthor().getIdLong());
        attempts = rs.getInt("tries");
        deaths = rs.getInt("deaths");
        jams = rs.getInt("jams");
        survivalRate = 100 - Math.round(rs.getDouble("deaths") / rs.getDouble("tries") * 100 * 10d) / 10d;
        streak = rs.getInt("streak");
    }

    private void setKarmaData() throws SQLException {
        int upVotes = kc.getUserRow(event.getAuthor().getIdLong()).getInt("upvotes");
        int downVotes = kc.getUserRow(event.getAuthor().getIdLong()).getInt("downvotes");
        karma = upVotes - downVotes;
    }

    private void setWalletData() throws SQLException {
        wallet = ec.getWealth(event.getAuthor().getIdLong());
    }
}

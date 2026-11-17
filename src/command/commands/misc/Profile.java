package command.commands.misc;

import command.Command;
import database.connectors.BangConnector;
import database.connectors.EconomyConnector;
import database.connectors.KarmaConnector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
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
    }

    @Override
    public void start(MessageReceivedEvent event) {
        this.event = event;
        eb = new EmbedBuilder();
        createProfile();
        // The picture card is the main view. If drawing it fails for any reason, the text embed still works.
        try {
            long user = event.getAuthor().getIdLong();
            command.util.misc.ProfileCard.Stats card = new command.util.misc.ProfileCard.Stats();
            card.name = event.getAuthor().getName();
            card.bangs = attempts;
            card.deaths = deaths;
            card.jams = jams;
            card.survival = survivalRate;
            card.streak = streak;
            card.karma = karma;
            card.achievements = economy.Achievements.unlocked(user).size();
            card.achievementsTotal = economy.Achievements.ALL.size();
            card.extras = economy.ProfileExtras.pills(user);
            card.avatar = fetchAvatar(event.getAuthor().getEffectiveAvatarUrl());
            byte[] png = command.util.misc.ProfileCard.render(card, economy.Cosmetics.loadout(user), economy.Cosmetics.owned(user), user);
            event.getChannel().sendFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(png, "profile.png")).queue();
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }

    private void createProfile() {
        try {
            setBangData();
            setKarmaData();
            setWalletData();
            buildEB();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    private void buildEB() {
        if (event.getMember() != null) {
            eb.setColor(event.getMember().getColor());
        } else {
            eb.setColor(Color.LIGHT_GRAY);
        }

        economy.Cosmetics.Extras extras = economy.Cosmetics.extras(event.getAuthor().getIdLong());
        if (extras.color != null) eb.setColor(extras.color);
        eb.setTitle(event.getAuthor().getName() + extras.title + "'s Profile");
        eb.setThumbnail(event.getAuthor().getAvatarUrl());

        eb.addField("Bangs", "" + attempts, true);
        eb.addField("Deaths", "" + deaths, true);
        eb.addField("Jams", "" + jams, true);
        eb.addField("Survival Rate", survivalRate + "%", true);
        eb.addField("Streak", streak + (streak > 0 ? " :fire:" : ""), true);
        eb.addField("Karma", "" + karma, false);
        eb.addField("Wallet", wallet + " *gc*", false);
        try {
            java.util.List<String> cosmetics = economy.CosmeticShop.getInstance()
                    .owned(event.getAuthor().getIdLong());
            if (!cosmetics.isEmpty()) eb.addField("Owned", String.join("\n", cosmetics), false);
            String worn = economy.Cosmetics.extras(event.getAuthor().getIdLong()).badges.toString();
            if (!worn.isEmpty()) eb.addField("Equipped", worn, false);
        } catch (SQLException e) {
            e.printStackTrace(); // cosmetics are optional on the profile
        }
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

    /** Downloads the avatar for the card; a missing or slow avatar just means the card shows an initial. */
    private static java.awt.image.BufferedImage fetchAvatar(String url) {
        if (url == null) return null;
        try {
            java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url + "?size=256").openConnection();
            c.setConnectTimeout(3000);
            c.setReadTimeout(3000);
            c.setRequestProperty("User-Agent", "gryph-discord-bot");
            try (java.io.InputStream in = c.getInputStream()) {
                return javax.imageio.ImageIO.read(in);
            }
        } catch (Exception e) {
            return null;
        }
    }
}

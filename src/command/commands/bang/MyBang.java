package command.commands.bang;

import command.Command;
import command.util.cache.BangCache;
import database.connectors.BangConnector;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.ResultSet;

public class MyBang extends Command {

    private BangConnector bc;

    /**
     * Initializes the command's key to "!mybang".
     */
    public MyBang() {
        super("!mybang", true);
        bc = new BangConnector();
    }

    /**
     * Prints the user's number of attempts, deaths, jams and total survival rate
     * in bang.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        if (event.getChannel().getIdLong() != 674369527731060749L
                && event.getChannel().getIdLong() != 551828950871965696L) {
            return;
        }

        int attempts = 0, deaths = 0, jams = 0, streak = 0;
        double survivalRate = 0;
        BangCache.updateAll();
        try {
            ResultSet rs = bc.getUserRow(event.getAuthor().getIdLong());
            attempts = rs.getInt("tries");
            deaths = rs.getInt("deaths");
            jams = rs.getInt("jams");
            survivalRate = 100 - Math.round(rs.getDouble("deaths") / rs.getDouble("tries") * 100 * 10d) / 10d;
            streak = rs.getInt("streak");
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        } finally {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.YELLOW);
            eb.setTitle(event.getAuthor().getName() + "'s Scores");

            eb.addField("Attempts",         attempts, true);
            eb.addField("Deaths",           deaths, true);
            eb.addField("Jams",             jams, true);
            eb.addField("Survival Rate",    survivalRate + "%", false);
            eb.addField("Streak",           streak + (streak > 0 ? " :fire:" : ""), true);

            eb.setThumbnail(event.getAuthor().getAvatarUrl());

            event.getChannel().sendMessage(eb.build()).queue();
        }
    }
}

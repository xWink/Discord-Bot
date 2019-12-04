package command.commands;

import command.Command;
import database.connectors.BangConnector;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.ResultSet;

public class MyBang extends Command {

    private BangConnector bc;

    /**
     * Initializes the command's key to "!mybang".
     */
    public MyBang() {
        super("!mybang", false);
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
        int attempts = 0, deaths = 0, jams = 0;
        double survivalRate = 0;

        try {
            ResultSet rs = bc.getUserRow(event.getAuthor().getIdLong());
            attempts = rs.getInt("tries");
            deaths = rs.getInt("deaths");
            jams = rs.getInt("jams");
            survivalRate = 100 - Math.round(rs.getDouble("deaths") / rs.getDouble("tries") * 100 * 10d) / 10d;
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        } finally {
            event.getChannel().sendMessage("**" + event.getAuthor().getName() + "'s scores**"
                    + "\nAttempts: " + attempts
                    + "\nDeaths: " + deaths
                    + "\nJams: " + jams
                    + "\nSurvival rate: " + survivalRate + "%").queue();
        }
    }
}

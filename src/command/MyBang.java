package command;

import database.BangConnector;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.ResultSet;

public class MyBang extends Command {

    private BangConnector bc;

    /**
     * Initializes the command's key to "!mybang".
     */
    MyBang() {
        super("!mybang");
        bc = new BangConnector();
    }

    /**
     * Key matches if the string equals exactly (ignoring case) the key.
     *
     * @param string the user's input being compared to the key
     * @return true if the key matches the string
     */
    @Override
    public boolean keyMatches(String string) {
        return super.keyMatches(string);
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
            survivalRate = 100 - Math.round(rs.getDouble("death_rate") * 100); //TODO: CHECK IF THIS IS CORRECT
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

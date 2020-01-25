package command.commands.bang;

import command.Command;
import database.connectors.BangConnector;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Daily extends Command {

    private BangConnector bc;

    /**
     * Initializes the command's key to "!daily".
     */
    public Daily() {
        super("!daily", false);
        bc = new BangConnector();
    }

    /**
     * Messages the time at which a user's bang daily resets.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        SimpleDateFormat df = new SimpleDateFormat("h:mm a"); // Set format of date/time
        TimeZone zone = TimeZone.getTimeZone("America/New_York"); // Get timezone
        df.setTimeZone(zone); // Apply timezone to format
        try {
            long resetTime = bc.getDaily(event.getAuthor().getIdLong());
            if (resetTime <= new Date().getTime()) {
                event.getChannel().sendMessage("Your daily reward is available now! Say `!bang`").queue();
            } else {
                event.getChannel().sendMessage("Your next daily reward is available at "
                        + df.format(new Date(resetTime))).queue();
            }
        } catch (SQLException e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

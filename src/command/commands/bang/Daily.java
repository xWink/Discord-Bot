package command.commands.bang;

import command.Command;
import economy.DailyService;
import main.Server;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Daily extends Command {

    /**
     * Initializes the command's key to "!daily".
     */
    public Daily() {
        super("!daily", true);
    }

    /**
     * Messages the time at which a user's bang daily resets.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        if (event.getChannel().getIdLong() != Server.SPAM_CHANNEL_ID
                && event.getChannel().getIdLong() != Server.BOTS_CHANNEL_ID) {
            return;
        }

        SimpleDateFormat df = new SimpleDateFormat("h:mm a"); // Set format of date/time
        TimeZone zone = TimeZone.getTimeZone("America/New_York"); // Get timezone
        df.setTimeZone(zone); // Apply timezone to format

        try {
            long resetTime = DailyService.getInstance().nextEligibleAt(event.getAuthor().getIdLong());
            if (resetTime <= new Date().getTime()) {
                if (event.getChannel().getIdLong() == Server.SPAM_CHANNEL_ID) {
                    event.getChannel().sendMessage("Your daily reward is available now! Say `!bang`").queue();
                } else {
                    event.getChannel().sendMessage("Your daily reward is available now!"
                            + " Go to <#674369527731060749> and say `!bang`").queue();
                }
            } else {
                event.getChannel().sendMessage("Your next daily reward is available at "
                        + df.format(new Date(resetTime))).queue();
            }
        } catch (SQLException e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

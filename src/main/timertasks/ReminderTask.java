package main.timertasks;

import courses.Reminders;
import courses.Reminders.Reminder;
import main.Server;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.TimerTask;

/** Sends the reminders that have come due: a direct message, or a post in the channel if that is where they were made. */
public class ReminderTask extends TimerTask {

    /** A reminder this much later than asked (the bot was off) says so. */
    private static final long LATE_MS = 2 * 60 * 1000;

    @Override
    public void run() {
        try {
            for (Reminder r : Reminders.claimDue(System.currentTimeMillis(), 50)) deliver(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String message(Reminder r, long now) {
        String late = now - r.due > LATE_MS ? " (a little late: I was offline)" : "";
        return "⏰ Reminder" + late + ": " + command.commands.course.Remind.defang(r.text);
    }

    private void deliver(Reminder r) {
        String text = message(r, System.currentTimeMillis());
        if (r.here) {
            toChannel(r, "<@" + r.user + "> " + text);
            return;
        }
        Server.API.retrieveUserById(r.user).queue(
                user -> user.openPrivateChannel().queue(
                        dm -> dm.sendMessage(text).queue(null, err -> toChannel(r, "<@" + r.user + "> I could not DM you. " + text)),
                        err -> toChannel(r, "<@" + r.user + "> I could not DM you. " + text)),
                err -> toChannel(r, "<@" + r.user + "> " + text));
    }

    private void toChannel(Reminder r, String text) {
        TextChannel channel = Server.API.getTextChannelById(r.channel);
        if (channel != null) {
            channel.sendMessage(text).setAllowedMentions(java.util.EnumSet.of(net.dv8tion.jda.api.entities.Message.MentionType.USER)).queue();
        }
    }
}

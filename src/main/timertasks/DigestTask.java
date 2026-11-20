package main.timertasks;

import courses.Digest;
import courses.DigestStore;
import courses.ReminderTime;
import main.Server;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.ZonedDateTime;
import java.util.TimerTask;

/** Sends each opted-in person's morning summary at their time, once a day, only when there is something in it. */
public class DigestTask extends TimerTask {

    @Override
    public void run() {
        try {
            ZonedDateTime now = ZonedDateTime.now(ReminderTime.ZONE);
            int minute = now.getHour() * 60 + now.getMinute();
            for (long user : DigestStore.claimDue(now.toLocalDate().toEpochDay(), minute)) {
                EmbedBuilder digest = Digest.build(user, now);
                if (digest == null) continue;
                Server.API.retrieveUserById(user).queue(u -> u.openPrivateChannel().queue(
                        dm -> dm.sendMessageEmbeds(digest.build()).queue(ok -> { }, err -> { }), err -> { }), err -> { });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

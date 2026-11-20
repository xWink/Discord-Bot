package command.commands.course;

import command.Command;
import courses.Digest;
import courses.DigestStore;
import courses.ReminderTime;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A morning summary by direct message. {@code !digest on} (at 8:00) or {@code !digest on 7:30}, {@code !digest off},
 * {@code !digest} for the setting and {@code !digest now} to see today's right away. It only sends on days when there
 * is something to say.
 */
public class DigestCommand extends Command {

    private static final Pattern CLOCK = Pattern.compile("(?i)(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?");

    public DigestCommand() {
        super("!digest", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().matches("^!digest(\\s+.*)?$");
    }

    static String show(int minute) {
        int h = minute / 60;
        int m = minute % 60;
        return (h % 12 == 0 ? 12 : h % 12) + ":" + (m < 10 ? "0" : "") + m + (h < 12 ? " am" : " pm");
    }

    /** @return the minute of the day, or -1 if the text is not a time */
    static int parseMinute(String text) {
        Matcher m = CLOCK.matcher(text.trim());
        if (!m.matches()) return -1;
        int hour = Integer.parseInt(m.group(1));
        int minute = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        String ampm = m.group(3);
        if (minute > 59) return -1;
        if (ampm != null) {
            if (hour < 1 || hour > 12) return -1;
            hour = hour % 12 + (ampm.equalsIgnoreCase("pm") ? 12 : 0);
        } else if (hour > 23) {
            return -1;
        }
        return hour * 60 + minute;
    }

    @Override
    public void start(MessageReceivedEvent event) {
        long user = event.getAuthor().getIdLong();
        String args = event.getMessage().getContentRaw().trim().replaceFirst("(?i)^!digest\\s*", "").trim();
        String[] words = args.split("\\s+", 2);
        try {
            switch (words[0].toLowerCase()) {
                case "on": {
                    int minute = words.length > 1 ? parseMinute(words[1]) : DigestStore.DEFAULT_MINUTE;
                    if (minute < 0) {
                        event.getChannel().sendMessage("I could not read that time. Say `!digest on` (8:00 am) or `!digest on 7:30` "
                                + "or `!digest on 6pm`.").queue();
                        return;
                    }
                    DigestStore.set(user, true, minute);
                    event.getChannel().sendMessage("Done: I will message you a summary at **" + show(minute) + "** Guelph time on days "
                            + "when there is something to say (due items, reminders, calendar dates). `!digest now` shows today's, "
                            + "`!digest off` stops it. Make sure you accept direct messages from this server.").queue();
                    return;
                }
                case "off":
                    DigestStore.set(user, false, Math.max(DigestStore.minute(user), DigestStore.DEFAULT_MINUTE));
                    event.getChannel().sendMessage("Done: no more morning summaries.").queue();
                    return;
                case "now": {
                    EmbedBuilder eb = Digest.build(user, ZonedDateTime.now(ReminderTime.ZONE));
                    if (eb == null) {
                        event.getChannel().sendMessage("Nothing to report: nothing is due soon, no reminders in the next day and no calendar "
                                + "dates in the next three days.").queue();
                        return;
                    }
                    GradeCommands.privately(event, eb.build(), "Here is your summary.");
                    return;
                }
                case "":
                case "status": {
                    int minute = DigestStore.minute(user);
                    event.getChannel().sendMessage(minute < 0 ? "Your morning summary is **off**. `!digest on` sends one at 8:00 am, "
                            + "`!digest on 7:30` at a time you choose." : "Your morning summary is **on**, at **" + show(minute) + "**. "
                            + "`!digest off` stops it.").queue();
                    return;
                }
                default:
                    event.getChannel().sendMessage("Say `!digest on [time]`, `!digest off`, `!digest now` or `!digest`.").queue();
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

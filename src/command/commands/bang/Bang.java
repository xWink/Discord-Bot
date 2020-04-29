package command.commands.bang;

import command.Command;
import command.util.cache.BangCache;
import command.util.cache.BangUpdate;
import database.connectors.BangConnector;
import database.connectors.EconomyConnector;
import main.Server;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Random;

public class Bang extends Command {

    private BangConnector bc;
    private EconomyConnector ec;
    private static int chambers;
    private static boolean jammed;
    private static boolean killed;
    private static boolean reward;
    private static boolean streak;
    private long userId;

    /**
     * Initializes the command's key to "!bang".
     * This command is "global" in the sense that it can be detected anywhere
     * HOWEVER it is only usable in the #bot-spam channel.
     */
    public Bang() {
        super("!bang", true);
        bc = new BangConnector();
        ec = new EconomyConnector();
        resetBools();
        resetChambers();
    }

    private void resetBools() {
        killed = false;
        jammed = false;
        reward = false;
    }

    private void resetChambers() {
        chambers = 6;
    }

    /**
     * Attempts to kill a user. The user may survive if the gun jams.
     */
    private void tryToKill() {
        if (chambers == 1) {
            jammed = new Random().nextInt(10) == 0;
            killed = !jammed;
        } else {
            killed = true;
        }
        resetChambers();
    }

    /**
     * Returns the string output of the result of the bang command.
     *
     * @param event the event that triggered the command
     * @return the string that describes the results of bang
     */
    private String getOutput(MessageReceivedEvent event) throws SQLException {
        String output = "";
        String poggers = "<:poggers:554666728878112774>";
        String poggies = "<:poggies:564285288621539328>";
        String name = event.getAuthor().getName();

        if (jammed)
            output += "The gun jammed... " + name + " survived " + poggers + poggers + poggers;
        else if (killed)
            output += "Bang! " + name + " died :skull:";
        else
            output += "Click. " + name + " survived  " + poggies;
        output += "\nChambers left in the cylinder: ||  " + chambers + "  ||\n";

        if (reward)
            output += name + " received their daily reward of 5 GryphCoins!\n";
        if (jammed)
            output += name + " received a bonus 50 GryphCoins!\n";

        int currentStreak = bc.getCurrentStreak(userId);
        if (streak && currentStreak > 0 && currentStreak % 10 == 0)
            output += name + " received an extra 50 GryphCoins for their streak bonus!\n";

        return output;
    }

    /**
     * Plays Russian Roulette. The gun may jam on the last round.
     * If the gun fires or jams, chambers is reset back to 6,
     * in any other case, chambers is decremented.
     * Info about the attempt is added to the bang table.
     * Rewards associated with the attempt are added to the economy table.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        long channelId = channel.getIdLong();

        if (channelId != Server.SPAM_CHANNEL_ID) {
            if (channelId == Server.BOTS_CHANNEL_ID) {
                channel.sendMessage("This command is only available in <#" + Server.SPAM_CHANNEL_ID + ">").queue();
            }
            return;
        }

        userId = event.getAuthor().getIdLong();
        int pull = new Random().nextInt(chambers);
        if (pull == 0) tryToKill();
        else chambers--;

        try {
            BangCache cache = BangCache.getInstance();
            long lastPlayed = bc.getUserRow(userId).getLong("last_played");
            LocalDate lastPlayedDate = Instant.ofEpochMilli(lastPlayed).atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate now = Instant.ofEpochMilli(new Date().getTime()).atZone(ZoneId.systemDefault()).toLocalDate();

            reward = bc.isEligibleForDaily(userId);
            streak = (now.minusDays(1).getDayOfYear() == lastPlayedDate.getDayOfYear()
                            && now.minusDays(1).getYear() == lastPlayedDate.getYear())
                    || bc.getUserRow(userId).getInt("streak") == 0;

            cache.enqueue(new BangUpdate(
                    userId,
                    new Date().getTime(), 1,
                    killed ? 1 : 0,
                    jammed ? 1 : 0,
                    reward,
                    streak));

            giveRewards();

            if (!cache.isPanicking()) {
                channel.sendMessage(getOutput(event)).queue();
            }

            resetBools();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    /**
     * Gives the user their rewards based on current streak,
     * whether they received their daily, and whether they jammed.
     *
     * @throws SQLException may be thrown when accessing database
     */
    private void giveRewards() throws SQLException {
        int totalReward = 0;
        int currentStreak = bc.getCurrentStreak(userId);

        if (streak && currentStreak > 0 && currentStreak % 10 == 0)
            totalReward += 50;

        if (reward)
            totalReward += 5;

        if (jammed)
            totalReward += 50;

        if (totalReward > 0)
            ec.addOrRemoveMoney(userId, totalReward);
    }
}

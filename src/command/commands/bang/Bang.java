package command.commands.bang;

import command.Command;
import command.util.cache.BangCache;
import command.util.cache.BangUpdate;
import database.connectors.BangConnector;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
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

    /**
     * Initializes the command's key to "!bang".
     */
    public Bang() {
        super("!bang", false);
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
    private String getOutput(MessageReceivedEvent event) {
        String output = "";
        String poggers = "<:poggers:554666728878112774>";
        String poggies = "<:poggies:564285288621539328>";

        if (jammed) output += "The gun jammed... " + event.getAuthor().getName()
                + " survived " + poggers + poggers + poggers;
        else if (killed) output += "Bang! " + event.getAuthor().getName() + " died :skull:";
        else output += "Click. " + event.getAuthor().getName() + " survived  " + poggies;

        output += "\nChambers left in the cylinder: ||  " + chambers + "  ||";

        if (reward) output += "\n" + event.getAuthor().getName()
                    + " received their daily reward of 5 GryphCoins!\n";
        if (jammed) output += "\n" + event.getAuthor().getName()
                    + " received a bonus 50 GryphCoins!";

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
        int pull = new Random().nextInt(chambers);
        if (pull == 0) tryToKill();
        else chambers--;

        try {
            boolean oldPanic = BangCache.isPanicking();
            long lastPlayed = bc.getUserRow(event.getAuthor().getIdLong()).getLong("last_played");
            LocalDate lastPlayedDate = Instant.ofEpochMilli(lastPlayed).atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate now = Instant.ofEpochMilli(new Date().getTime()).atZone(ZoneId.systemDefault()).toLocalDate();

            reward = bc.isEligibleForDaily(event.getAuthor().getIdLong());
            streak = (now.minusDays(1).getDayOfYear() == lastPlayedDate.getDayOfYear()
                    && now.minusDays(1).getYear() == lastPlayedDate.getYear())
                    || bc.getUserRow(event.getAuthor().getIdLong()).getInt("streak") == 0;

            BangCache.enqueue(new BangUpdate(event.getAuthor().getIdLong(),
                    new Date().getTime(), 1,
                    killed ? 1 : 0, jammed ? 1 : 0, reward, streak));

            if (reward) ec.addOrRemoveMoney(event.getAuthor().getIdLong(), 5);
            if (jammed) ec.addOrRemoveMoney(event.getAuthor().getIdLong(), 50);

            if (!BangCache.isPanicking()) {
                if (oldPanic) {
                    event.getChannel().sendMessage(BangCache.getQueueResults()).queue();
                } else {
                    event.getChannel().sendMessage(getOutput(event)).queue();
                }
                BangCache.updateAll();
            }

            resetBools();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

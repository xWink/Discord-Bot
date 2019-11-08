package command;

import database.BangConnector;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Random;

public class Bang extends Command {

    private BangConnector bc;
    private static int chambers;
    private static boolean jammed;
    private static boolean killed;
    private static boolean reward;

    /**
     * Initializes the command's key to "!bang".
     */
    Bang() {
        super("!bang");
        bc = new BangConnector();
        resetJammed();
        resetKilled();
        resetChambers();
        resetReward();
    }


    private void resetKilled() {
        killed = false;
    }

    private void resetJammed() {
        jammed = false;
    }

    private void resetChambers() {
        chambers = 6;
    }

    private void resetReward() {
        reward = false;
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
    }


    /**
     * Returns the string output of the result of the bang command.
     *
     * @param event the event that triggered the command
     * @return the string that describes the results of bang
     */
    private String getOutput(MessageReceivedEvent event) {
        String poggers = "<:poggers:564285288621539328>";
        if (jammed) return "The gun jammed... " + event.getAuthor().getName()
                + " survived " + poggers + poggers + poggers;
        else if (killed) return "bang! " + event.getAuthor().getName() + " died :skull:";
        else return "Click. " + event.getAuthor().getName() + " survived  <:poggies:564285288621539328>";
    }


    /**
     * Key matches if the string equals exactly (ignoring case) the key.
     *
     * @param string the user's input being compared to the key
     * @return true if the key matches the string
     */
    @Override
    public boolean keyMatches(String string) {
        return string.equalsIgnoreCase(getKey());
    }


    /**
     * Plays Russian Roulette. The gun may jam on the last round.
     * Info about the attempt is added to the bang table.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        int pull = new Random().nextInt(chambers);
        if (pull == 0) tryToKill();
        chambers = chambers == 1 ? 6 : chambers - 1;
        try {
            reward = bc.isEligibleForDaily(event.getAuthor().getIdLong());
            bc.updateUserRow(event.getAuthor().getIdLong(), jammed, killed, reward);
            //TODO: Update reward in economy
            String output = getOutput(event);
            output += "Chambers left in the cylinder: ||  " + chambers + "  ||";
            event.getChannel().sendMessage(output).queue();
            resetReward();
            resetKilled();
            resetJammed();
        } catch (Exception e) {
            e.printStackTrace();
            event.getChannel().sendMessage("An error occurred with Bang. Please contact a moderator!").queue();
        }
    }
}

package command.commands.misc;

import command.Command;
import database.connectors.JackPotConnector;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Date;
import java.util.Random;

public class Lotto extends Command {

    private JackPotConnector jc;
    private EconomyConnector ec;
    private static int jackpot;
    private static int players;
    private static long time;
    /**
     * Initializes the command's key to "!flip".
     */
    public Lotto() {
        super("!lotto", false);
        ec = new EconomyConnector();
        jc = new JackPotConnector();
        this.resetLotto();
    }

    /*
     * resets the lottery
     */
    public void resetLotto() {
        jackpot = 0;
        players = 0;
        time = 0;
        jc.clearTable();
    }

    @Override
    public void start(MessageReceivedEvent event) {
        if (!verifyInput(event)) return;
        try {
            if (!ec.canAfford(event.getAuthor().getIdLong(), 20)) {
                event.getChannel().sendMessage("You do not have enough money to run in the Lottery!\n"
                        + "Your wallet contains " + ec.getWealth(event.getAuthor().getIdLong()) + " GryphCoins").queue();
                return;
            }
            if (players == 0) time = new Date().getTime();
            jc.addLotto(event.getAuthor().getIdLong(),players, event.getAuthor().getName());
            event.getChannel().sendMessage("Your Lotto number is " + (players)).queue();
            players++;
            runLotto(event);
        }catch(Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
    public boolean verifyInput(MessageReceivedEvent event){
        long userId = event.getAuthor().getIdLong();
        if(jc.userInLotto(userId)){
            event.getChannel().sendMessage("Your already in the Lottery").queue();
            return false;
        }
        return true;
    }

    /*
    * checks who won if the date has passed
    *@param event the event for the command
     */
    public void runLotto(MessageReceivedEvent event) {
        try {
            if (players > 1 &&  new Date().getTime() - time >= 86400000 ) {
                int winner = new Random().nextInt(players -1);
                long winnerId = jc.getWinnerID(winner);
                String winnerName = jc.getWinnerName(winner);
                event.getChannel().sendMessage("The Winner of the Lottery is:\n" + winnerId
                        + "\nwinning " + jackpot + "gc").queue();
                ec.addOrRemoveMoney(winnerId, jackpot);
                resetLotto();
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}
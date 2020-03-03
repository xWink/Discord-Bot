package main;

import main.eventlisteners.ConnectionEventListener;
import main.eventlisteners.JoinLeaveEventListener;
import main.eventlisteners.MessageEventListener;
import main.eventlisteners.ReactionEventListener;
import main.timertasks.DiscussionPurge;
import main.timertasks.PruneBangStreaks;
import main.timertasks.RemoveExpiredRoles;
import main.timertasks.UpdateHighScores;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class RoleBot {

    private static final Logger logger = LogManager.getLogger(RoleBot.class);

    /**
     * Acquires settings from config file, activates bot based on token in file,
     * and adds the needed EventListeners.
     * @param args ignored
     */
    public static void main(String[] args) {
        BasicConfigurator.configure();
        try {
            logger.debug("Entering application.");
            Server.getApi().addEventListener(
                    MessageEventListener.getMessageEventListener(),
                    new ReactionEventListener(),
                    new ConnectionEventListener(),
                    new JoinLeaveEventListener());
            Timer timer = new Timer();
            timer.schedule(new RemoveExpiredRoles(), 1000 * 60 * 60, 1000 * 60 * 60);
            timer.schedule(new UpdateHighScores(), 1000 * 60 * 30, 1000 * 60 * 30);
            timer.schedule(new PruneBangStreaks(), 1000 * 60 * 60 * 2, 1000 * 60 * 60 * 2);
            //startPurgeScheduler();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the scheduler for the weekly Discussion purge (Monday at 11AM).
     */
    private static void startPurgeScheduler() {
        Calendar with = Calendar.getInstance();
        Map<Integer, Integer> dayToDelay = new HashMap<>();
        dayToDelay.put(Calendar.FRIDAY, 2);
        dayToDelay.put(Calendar.SATURDAY, 1);
        dayToDelay.put(Calendar.SUNDAY, 0);
        dayToDelay.put(Calendar.MONDAY, 6);
        dayToDelay.put(Calendar.TUESDAY, 5);
        dayToDelay.put(Calendar.WEDNESDAY, 4);
        dayToDelay.put(Calendar.THURSDAY, 3);

        int dayOfWeek = with.get(Calendar.DAY_OF_WEEK);
        int hour = with.get(Calendar.HOUR_OF_DAY);
        int delayInDays = dayToDelay.get(dayOfWeek);
        int delayInHours;

        if (delayInDays == 6 && hour < 11) {
            delayInHours = 11 - hour;
        } else {
            delayInHours = delayInDays * 24  + ((24 - hour) + 11);
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new DiscussionPurge(), delayInHours,
                179, TimeUnit.HOURS);
    }

    private void updateWagers() {
        /* Check wagers every 5 minutes
        Timer timer2 = new Timer();
        TimerTask wagerTask = new TimerTask() {
            @Override
            public void run() {
                Date date = new Date();
                ListOfWagers wagers = new ListOfWagers();
                for (Wager w : wagers.getWagers()) {
                    if (w.getCreationTime() >= date.getTime()) {
                        wagers.removeWager(w);
                    }
                }
            }
        };
        timer2.schedule(wagerTask,1000*60*5, 1000*60*60);
        */
    }
}

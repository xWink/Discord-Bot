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
}

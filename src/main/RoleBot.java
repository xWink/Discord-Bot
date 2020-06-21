package main;

import main.eventlisteners.EventListeners;
import main.timertasks.CleanMessageTable;
import main.timertasks.PruneBangStreaks;
import main.timertasks.RemoveExpiredRoles;
import main.timertasks.UpdateHighScores;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.Timer;


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
            Server.API.addEventListener(EventListeners.ALL.toArray());
            Timer timer = new Timer();
            timer.schedule(new RemoveExpiredRoles(), 1000 * 60 * 60, 1000 * 60 * 60);
            timer.schedule(new UpdateHighScores(), 1000 * 60 * 30, 1000 * 60 * 30);
            timer.schedule(new PruneBangStreaks(), 1000 * 60 * 60 * 2, 1000 * 60 * 60 * 2);
            timer.schedule(new CleanMessageTable(), 0, 1000 * 60 * 60 * 24);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package command.util.cache;

import database.connectors.BangConnector;
import main.Server;

import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public final class BangCache {

    private static Queue<BangUpdate> queue;
    private static BangConnector bc;
    private static boolean panic;
    private static Queue<Long> last20;

    static {
        queue = new LinkedList<>();
        panic = false;
        bc = new BangConnector();
        last20 = new LinkedList<>();
    }

    private BangCache() {

    }

    /**
     * Adds data from a BangUpdate object to the queue.
     *
     * @param update the object containing data about an update to the database
     */
    public static void enqueue(BangUpdate update) {
        for (BangUpdate element : queue) {
            if (element.getId() == update.getId()) {
                element.addAttempts(update.getAttempts());
                element.addDeaths(update.getDeaths());
                element.addJams(update.getJams());
                element.setLastPlayed(new Date().getTime());
                if (update.isRewarded()) element.setReward(true);
                return;
            }
        }
        queue.add(update);

        last20.add(update.getLastPlayed());
        if (last20.size() > 20) last20.remove();

        checkPanic();
    }

    private static void checkPanic() {
        long avgTime = 0;
        boolean oldPanic = panic;
        for (Long update : last20) {
            avgTime += update;
        }
        avgTime /= last20.size();

        panic = avgTime > new Date().getTime() - 8000 && last20.size() >= 10;

        if (panic && !oldPanic) System.out.println("Panic mode: activated");
        else if (!panic && oldPanic) System.out.println("Panic mode: deactivated");
    }

    /**
     * Dequeue every element in the cache and send the the SQL update of that element
     * to the database.
     */
    public static void updateAll() {
        try {
            while (!queue.isEmpty()) {
                bc.customUpdate(dequeue().toSQL());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static BangUpdate dequeue() {
        return queue.remove();
    }

    /**
     * Checks if the cache is in panic mode.
     *
     * @return true if in panic mode
     */
    public static boolean isPanicking() {
        return panic;
    }

    /**
     * Gets a string of all the results of every BangUpdate in the cache.
     *
     * @return a string containing all of the results in the cache
     */
    public static String getQueueResults() {
        String output = "**Combined Data:**\n";
        for (BangUpdate update : queue) {
            output = output.concat("**" + Server.getGuild().getMemberById(update.getId()).getNickname() + ":**\n"
                    + "Attempts: " + update.getAttempts() + "\n"
                    + "Deaths : " + update.getDeaths() + "\n"
                    + "Jams: " + update.getJams() + "\n\n");
        }
        return output;
    }
}

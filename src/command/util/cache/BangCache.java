package command.util.cache;

import database.connectors.BangConnector;

import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public final class BangCache {

    private static Queue<BangUpdate> queue;
    private static boolean panic;
    private static BangConnector bc;

    static {
        queue = new LinkedList<>();
        panic = false;
        bc = new BangConnector();
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

        checkPanic();
        if (!panic) updateAll();
    }

    private static void checkPanic() {
        long avgTime = 0;
        boolean oldPanic = panic;
        for (BangUpdate update : queue) {
            avgTime += update.getLastPlayed();
        }
        avgTime /= queue.size();

        panic = avgTime > new Date().getTime() - 5000;

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
}

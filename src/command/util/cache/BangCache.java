package command.util.cache;

import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public final class BangCache {

    private static Queue<BangUpdate> queue;
    private static Date[] last20Updates;
    private static boolean panic;

    static {
        queue = new LinkedList<>();
        last20Updates = new Date[20];
        panic = false;
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
                if (update.isRewarded()) element.setReward(true);
                return;
            }
        }
        queue.add(update);
    }

    private static BangUpdate dequeue() {
        return queue.remove();
    }
}

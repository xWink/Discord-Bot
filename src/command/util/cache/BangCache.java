package command.util.cache;

import java.util.LinkedList;
import java.util.Queue;

public final class BangCache {

    private static Queue<BangUpdate> queue;

    static {
        queue = new LinkedList<>();
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

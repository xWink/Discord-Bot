package command.util.cache;

import database.connectors.BangConnector;
import main.Server;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.*;

public final class BangCache {

    private static BangCache bangCache;

    private static final long THRESHOLD = 12000;

    private Timer timer;
    private static Queue<BangUpdate> queue;
    private static BangConnector bc;
    private static boolean panic;
    private static ArrayList<Long> last20;
    private static TextChannel channel;

    private BangCache() {
        queue = new LinkedList<>();
        panic = false;
        bc = new BangConnector();
        last20 = new ArrayList<>();
        channel = Server.getApi().getTextChannelById(674369527731060749L);
    }

    public static BangCache getInstance() {
        if (bangCache == null)
            bangCache = new BangCache();
        return bangCache;
    }

    /**
     * Adds data from a BangUpdate object to the queue.
     *
     * @param update the object containing data about an update to the database
     */
    public void enqueue(BangUpdate update) {
        boolean found = false;
        for (BangUpdate element : queue) {
            if (element.getId() == update.getId()) {
                element.addAttempts(update.getAttempts());
                element.addDeaths(update.getDeaths());
                element.addJams(update.getJams());
                element.setLastPlayed(update.getLastPlayed());
                if (update.isRewarded()) element.setReward(true);
                if (update.isStreakIncreased()) element.setStreak(true);
                found = true;
            }
        }
        if (!found) queue.add(update);
        if (last20.size() > 19) last20.remove(0);
        last20.add(update.getLastPlayed());
        checkPanic();
    }

    private void checkPanic() {
        boolean oldPanic = panic;
        long avgTime = last20.stream().reduce(0L, Long::sum) / last20.size();

        panic = avgTime > new Date().getTime() - THRESHOLD && last20.size() >= 20;

        if (oldPanic) {
            timer.cancel();
            timer = new Timer();
            timer.schedule(new PanicTimer(), 1000 * 10);
        } else if (panic) {
            timer = new Timer();
            timer.schedule(new PanicTimer(), 1000 * 10);
        } else {
            updateAll();
        }
    }

    public boolean isPanicking() {
        return panic;
    }

    /**
     * Dequeue every element in the cache and send the the SQL update of that element
     * to the database.
     */
    public void updateAll() {
        try {
            while (!queue.isEmpty()) {
                bc.customUpdate(dequeue().toSQL());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BangUpdate dequeue() {
        return queue.remove();
    }

    /**
     * Prints a string of all the results of every BangUpdate in the cache to the bot channel.
     */
    public void printResults() {
        String output = "**Combined Data:**\n";
        for (BangUpdate update : queue) {
            User user = Server.getApi().getUserById(update.getId());
            if (user != null) {
                output = output.concat("**" + user.getName() + ":**\n"
                        + "Attempts: " + update.getAttempts() + "\n"
                        + "Deaths : " + update.getDeaths() + "\n"
                        + "Jams: " + update.getJams() + "\n"
                        + (update.isRewarded() ? "Daily received!\n\n" : ""));
            }
        }
        channel.sendMessage(output).queue();
    }

    private class PanicTimer extends TimerTask {
        @Override
        public void run() {
            timer.cancel();
            updateAll();
            printResults();
        }
    }
}

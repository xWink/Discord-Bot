package main.timertasks;

import database.connectors.MessagesConnector;

import java.util.TimerTask;

public class CleanMessageTable extends TimerTask {

    /**
     * Backs up all messages older than a day and removes them from
     * the messages table.
     *
     * @see MessagesConnector
     */
    @Override
    public void run() {
        final int pruneThreshold = 1000 * 60 * 60 * 24;
        MessagesConnector mc = new MessagesConnector();
        try {
            mc.backupMessages(pruneThreshold);
            mc.pruneMessages(pruneThreshold);
            System.out.println("Successfully backed up messages older than " + pruneThreshold + " ms");
        } catch (Exception e) {
            System.out.println("Failed to backup messages");
        }
    }
}

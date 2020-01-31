package main.timertasks;

import database.connectors.BangConnector;

import java.util.TimerTask;

public class PruneBangStreaks extends TimerTask {

    @Override
    public void run() {
        try {
            BangConnector bc = new BangConnector();
            bc.pruneStreaks();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

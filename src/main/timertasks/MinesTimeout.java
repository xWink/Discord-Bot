package main.timertasks;

import command.commands.economy.Mines;
import command.util.game.MinesGame;

import java.util.TimerTask;

/** Cashes out idle Mines rounds at their standing (the stake is simply released if nothing was uncovered). */
public class MinesTimeout extends TimerTask {

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (MinesGame game : Mines.openGames()) {
            if (now - game.getLastAction() < Mines.IDLE_LIMIT_MS) continue;
            try {
                synchronized (game) {
                    Mines.finish(game, game.netIfStopped());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

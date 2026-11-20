package main.timertasks;

import command.commands.economy.HiLo;
import command.util.game.HiLoGame;

import java.util.TimerTask;

/**
 * Closes idle Higher or Lower rounds at their current standing (a forfeit if
 * nothing was won yet is not applied: the stake is simply released).
 */
public class HiLoTimeout extends TimerTask {

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (HiLoGame game : HiLo.openGames()) {
            if (now - game.getLastAction() < HiLo.IDLE_LIMIT_MS) continue;
            try {
                synchronized (game) {
                    HiLo.finish(game, game.netIfStopped());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

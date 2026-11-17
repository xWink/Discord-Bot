package main.timertasks;

import economy.DecayService;

import java.util.TimerTask;

/**
 * Settles overdue weekly decay for idle wallets so it applies at the fixed
 * cutoff whether or not the owner is active.
 */
public class DecaySweep extends TimerTask {

    @Override
    public void run() {
        try {
            int n = DecayService.sweep();
            if (n > 0) System.out.println("Decay settled for " + n + " wallets");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

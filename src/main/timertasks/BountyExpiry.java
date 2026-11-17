package main.timertasks;

import economy.BountyService;

import java.util.TimerTask;

/** Returns coins for bounties nobody finished in time. */
public class BountyExpiry extends TimerTask {
    @Override
    public void run() {
        try {
            int banners = economy.BannerService.getInstance().expireUnanswered();
            if (banners > 0) System.out.println("Refunded " + banners + " unanswered banner requests");
            int n = BountyService.getInstance().expireOverdue();
            if (n > 0) System.out.println("Expired " + n + " bounties");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

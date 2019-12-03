package main;

import main.eventlisteners.ConnectionEventListener;
import main.eventlisteners.JoinLeaveEventListener;
import main.eventlisteners.MessageEventListener;
import main.eventlisteners.ReactionEventListener;

public class RoleBot {

    /**
     * Acquires settings from config file, activates bot based on token in file,
     * and adds the needed EventListeners.
     * @param args ignored
     */
    public static void main(String[] args) {
        try {
            Server.getApi().addEventListener(new MessageEventListener());
            Server.getApi().addEventListener(new ReactionEventListener());
            Server.getApi().addEventListener(new ConnectionEventListener());
            Server.getApi().addEventListener(new JoinLeaveEventListener());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateBangScores() {
        /*Check bang high scores every hour
        Timer timer1 = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                UpdateRoles.updateBangRoles();
                UpdateRoles.removeExpiredColours();
            }
        };
        timer1.schedule(task,1000*60*60,1000*60*60);
        */
    }

    private void updateWagers() {
        /* Check wagers every 5 minutes
        Timer timer2 = new Timer();
        TimerTask wagerTask = new TimerTask() {
            @Override
            public void run() {
                Date date = new Date();
                ListOfWagers wagers = new ListOfWagers();
                for (Wager w : wagers.getWagers()) {
                    if (w.getCreationTime() >= date.getTime()) {
                        wagers.removeWager(w);
                    }
                }
            }
        };
        timer2.schedule(wagerTask,1000*60*5, 1000*60*60);
        */
    }
}

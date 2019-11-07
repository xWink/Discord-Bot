package main;

import main.eventlistener.ConnectionEventListener;
import main.eventlistener.JoinLeaveEventListener;
import main.eventlistener.MessageEventListener;
import main.eventlistener.ReactionEventListener;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

public class RoleBot {

	private static JDA api;

	/**
	 * Acquires settings from config file, activates bot based on token in file,
	 * and adds the needed EventListeners.
	 * @param args ignored
	 */
	public static void main(String[] args) {
		try {
			api = new JDABuilder(AccountType.BOT).setToken(Config.getToken()).build();
			api.addEventListener(new MessageEventListener());
			api.addEventListener(new ReactionEventListener());
			api.addEventListener(new ConnectionEventListener());
			api.addEventListener(new JoinLeaveEventListener());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static JDA getJDA() {
		return api;
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
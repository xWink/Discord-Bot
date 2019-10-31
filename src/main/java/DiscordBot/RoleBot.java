package DiscordBot;

import DiscordBot.util.tictactoe_util.ListOfWagers;
import DiscordBot.util.tictactoe_util.Wager;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import DiscordBot.util.database.UpdateRoles;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class RoleBot {

	public static ConfigFile config;
	public static JDA api;

	public static void main(String[] args){

		try {
			config = GetConfig.getConfig();

			// Create bot with token given by Discord developer page
			api = new JDABuilder(AccountType.BOT).setToken(config.token).build();
			api.addEventListener(new MyEventListener());

			// Check bang high scores every hour
			Timer timer1 = new Timer();
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					UpdateRoles.updateBangRoles();
					UpdateRoles.removeExpiredColours();
				}
			};
			timer1.schedule(task,1000*60*60,1000*60*60);

			// Check wagers every 5 minutes
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
		}
		
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
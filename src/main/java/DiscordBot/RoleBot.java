package DiscordBot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import DiscordBot.helpers.UpdateBangRoles;

import java.util.Timer;
import java.util.TimerTask;

public class RoleBot {

	public static ConfigFile config;

	public static void main(String[] args){

		try {
			config = GetConfig.getConfig();

			// Create bot with token given by Discord developer page
			JDA api = new JDABuilder(AccountType.BOT).setToken(config.token).build();
			api.addEventListener(new MyEventListener());
/*
			// Check Bang high scores every hour
			Timer timer = new Timer();
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					UpdateBangRoles.updateBangRoles();
				}
			};
			timer.schedule(task,1000*60*60,1000*60*60);
*/
		}
		
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
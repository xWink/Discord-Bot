package DiscordBot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import DiscordBot.util.misc.UpdateRoles;

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
			Timer timer = new Timer();
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					UpdateRoles.updateBangRoles();
					UpdateRoles.removeExpiredColours();
				}
			};
			timer.schedule(task,1000*60*60,1000*60*60);

		}
		
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
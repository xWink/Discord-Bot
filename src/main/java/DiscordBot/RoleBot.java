package DiscordBot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import DiscordBot.helpers.UpdateBangRoles;

import java.io.File;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class RoleBot {

	public static void main(String[] args) throws Exception{

		try {
			File file = new File("token.txt");
			Scanner scanner = new Scanner(file);
			String token = scanner.nextLine();
			scanner.close();
			// Create bot with token given by Discord developer page
			JDA api = new JDABuilder(AccountType.BOT).setToken(token).build();
			api.addEventListener(new MyEventListener());

			// Check Bang high scores every hour
			Timer timer = new Timer();
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					UpdateBangRoles.updateBangRoles();
				}
			};
			timer.schedule(task,1000*60*60,1000*60*60);
		}catch (Exception e){
			e.printStackTrace();
		}
	}
}
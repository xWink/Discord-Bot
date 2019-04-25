package DiscordBot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

import java.io.File;
import java.util.Scanner;
import DiscordBot.GetConfig;

public class RoleBot {
	public static void main(String[] args) throws Exception{

		try {
			/*
			File file = new File("token.txt");
			Scanner scanner = new Scanner(file);
			String token = scanner.nextLine();
			scanner.close();
			*/
			String token = GetConfig.getConfig();


			// Create bot with token given by Discord developer page
			JDA api = new JDABuilder(AccountType.BOT).setToken(token).build();
			api.addEventListener(new MyEventListener());
		}
		
		catch (Exception e){
			e.printStackTrace();
		}
	}
}
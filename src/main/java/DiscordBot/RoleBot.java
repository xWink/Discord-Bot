package DiscordBot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import java.util.Scanner;

public class RoleBot {
	public static void main(String[] args) throws Exception{

		try {
			Scanner scanner = new Scanner("token.txt");
			String token = scanner.nextLine();
			scanner.close();
			// Create bot with token given by Discord developer page
			JDA api = new JDABuilder(AccountType.BOT).setToken(token).build();
			api.addEventListener(new MyEventListener());
		}catch (Exception e){
			e.printStackTrace();
		}
	}
}
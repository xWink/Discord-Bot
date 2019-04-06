package DiscordBot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

public class RoleBot {
	public static void main(String[] args) throws Exception{

		try {
			// Create bot with token given by Discord developer page
			JDA api = new JDABuilder(AccountType.BOT).setToken("CHANGETOKEN").build();
			api.addEventListener(new MyEventListener());
		}catch (Exception e){
			e.printStackTrace();
		}
	}
}
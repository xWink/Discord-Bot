package DiscordBot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

import java.io.File;
import java.util.Scanner;

import DiscordBot.GetConfig;
import DiscordBot.ConfigFile;

public class RoleBot {
	public static ConfigFile config;

	public static void main(String[] args) throws Exception{

		try {
			config = GetConfig.getConfig();

			// Create bot with token given by Discord developer page
			JDA api = new JDABuilder(AccountType.BOT).setToken(config.token).build();
			api.addEventListener(new MyEventListener());
		}
		
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
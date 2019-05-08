package DiscordBot.commands;

import net.dv8tion.jda.core.entities.MessageChannel;


public class Scores {

	public static void scores(MessageChannel channel){

		channel.sendMessage("Sorry, this command is currently under maintenance!").queue();
/*
		// Print message
		channel.sendMessage("The best score is "+best+" ms from "+bestMem+"!").queue();
		channel.sendMessage("The worst score is "+worst+" ms from "+worstMem+"!").queue();
		*/
	}
}

package DiscordBot;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.Arrays;
import java.util.List;

import DiscordBot.commands.*;

public class MyEventListener extends ListenerAdapter {

	public static Guild guild;
	private int chamberCount = 6;
	private ConfigFile cfg = RoleBot.config;
	
  @Override
	public void onMessageReceived(MessageReceivedEvent event){

		final User author = event.getAuthor(); // Variable author is the author of type User
		if (author.isBot()){
			System.out.println(author+": "+event.getMessage().getContentRaw());
			return; // If the event is made by the bot, ignore it
		}

		final Message message = event.getMessage(); // Variable message is the detected message
		final String content = message.getContentRaw(); // Variable content is the text of the message
		final MessageChannel channel = event.getChannel(); // Variable channel is the text channel the message came from
		guild = event.getGuild(); // Variable guild is the Discord server
		final Member auth = guild.getMember(author); // Variable auth is author of type Member
	  	final List channels = Arrays.asList(cfg.channel);

		// Check if the bot is allowed to send messages in the current channel
		if ( !cfg.channel[0].equals("all") && !channels.contains(channel.getId())) return;

		// Bot shows how to use !join
		if (content.toLowerCase().equals("!join") || content.toLowerCase().equals("!join "))
			channel.sendMessage("Command: !join <courseID>\n\nExample: !join mcs2100").queue();

		// Bot shows how to use !leave
		else if (content.toLowerCase().equals("!leave") || content.toLowerCase().equals("!leave "))
			channel.sendMessage("Command: !leave <courseID>\n\nExample: !leave mcs2100").queue();

		// Bot shows how to use !join and !leave
		else if (content.toLowerCase().equals("!help"))
			Help.help(channel);

		// Bot responds with pong and latency
		else if (content.toLowerCase().equals("!ping"))
			Ping.ping(author, event, channel);

		// Bot creates new text channel and deletes old one (OWNER ONLY)
		else if (content.toLowerCase().equals("!totalchatwipe"))
			TotalChatWipe.chatWipe(auth, guild, channel);

		// Bot gives requested role to target (MODERATOR->PEASANT ONLY)
		else if(content.toLowerCase().startsWith("!giverole "))
			GiveRole.giveRole(auth, channel, guild, content, message);

		// Bot removes requested role from user (MODERATOR->PEASANT ONLY)
		else if(content.startsWith("!takerole "))
			TakeRole.takeRole(auth, channel, guild, content, message);

		// User requests to join/create an elective role
		else if(content.startsWith("!join "))
			Join.join(auth, author, channel, guild, content);

		// Remove user's application from CSV file
		else if (content.toLowerCase().startsWith("!leave "))
			Leave.leave(auth, author, channel, guild, content);

		// Delete all non-specified roles (OWNER ONLY)
		else if (content.toLowerCase().equals("!cleanroles"))
			CleanRoles.cleanRoles(auth, channel, guild);

		// Delete all elective channels (OWNER ONLY)
		else if(content.toLowerCase().equals("!cleanelectives"))
			CleanElectives.cleanElectives(auth, channel, guild);

		// Russian roulette
		else if (content.toLowerCase().equals("!bang"))
			chamberCount = Roulette.roulette(author, chamberCount,  channel);

		// Russian roulette scores
		else if (content.toLowerCase().equals("!bangscore") || content.toLowerCase().equals("!bangscores"))
			BangScores.bangScores(channel, guild);

		// Show bang scores for individual
		else if (content.toLowerCase().equals("!mybang"))
			MyBang.myBang(author, channel);

		// Show available Elective roles
		else if (content.toLowerCase().equals("!roles"))
			ShowRoles.showRoles(guild, channel);
	}
}

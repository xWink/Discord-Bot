package DiscordBot;

import DiscordBot.commands.admin_commands.*;
import DiscordBot.commands.bang.BangScores;
import DiscordBot.commands.bang.MyBang;
import DiscordBot.commands.bang.Roulette;
import DiscordBot.commands.blackjack.BlackJackCommands;
import DiscordBot.commands.groups.Join;
import DiscordBot.commands.groups.Leave;
import DiscordBot.commands.groups.ShowRoles;
import DiscordBot.commands.misc.Help;
import DiscordBot.commands.misc.MyWallet;
import DiscordBot.commands.misc.Ping;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import static DiscordBot.util.misc.DatabaseUtil.connect;

public class MyEventListener extends ListenerAdapter {

	public static Guild guild;
	private int chamberCount = 6;
	private ConfigFile cfg = RoleBot.config;

	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event){
		TextChannel generalChannel = guild.getTextChannelsByName("general", true).get(0);
		TextChannel botsChannel = guild.getTextChannelsByName("bots", true).get(0);

		generalChannel.sendMessage("Welcome " + event.getUser().getAsMention() +
				"! Feel free to ask any questions in " + generalChannel.getAsMention() +
				"\nIf you want to play with our bot, made in-house, go to " + botsChannel.getAsMention() +
				" and say `!help` :smiley:").queue();
	}

	@Override
	public void onDisconnect(DisconnectEvent event){
		System.out.println("Disconnected from server");
	}
	
  	@Override
  	public void onMessageReceived(MessageReceivedEvent event){

		final User author = event.getAuthor(); // Variable author is the author of type User
		if (author.isBot())
			return; // If the event is made by the bot, ignore it

		final Message message = event.getMessage(); // Variable message is the detected message
		final String content = message.getContentRaw(); // Variable content is the text of the message
		final TextChannel channel = event.getTextChannel(); // Variable channel is the text channel the message came from
		guild = event.getGuild(); // Variable guild is the Discord server
		final Member auth = guild.getMember(author); // Variable auth is author of type Member
	  	final List channels = Arrays.asList(cfg.channel);
	  	//final Market market = new Market(guild);
	  	Connection conn;

		// Check if the bot is allowed to send messages in the current channel
		if ( !cfg.channel[0].equalsIgnoreCase("all") && !channels.contains(channel.getId())) return;

		// Connect to database
		if ((conn = connect()) == null){
			channel.sendMessage("Could not connect to database. Please contact a moderator :(").complete();
			return;
		}

		// Bot shows how to use !join
		if (content.equalsIgnoreCase("!join") || content.equalsIgnoreCase("!join "))
			channel.sendMessage("Command: !join <courseID>\n\nExample: !join mcs2100").queue();

		// Bot shows how to use !leave
		else if (content.equalsIgnoreCase("!leave") || content.equalsIgnoreCase("!leave "))
			channel.sendMessage("Command: !leave <courseID>\n\nExample: !leave mcs2100").queue();

		// Bot shows how to use its commands
		else if (content.equalsIgnoreCase("!help"))
			Help.help(channel);

		// Bot responds with pong and latency
		else if (content.equalsIgnoreCase("!ping"))
			Ping.ping(author, event, channel);

		// Bot creates new text channel and deletes old one (OWNER ONLY)
		else if (content.equalsIgnoreCase("!totalchatwipe"))
			TotalChatWipe.chatWipe(auth, guild, channel);

		// Bot gives requested role to target (MODERATOR->PEASANT ONLY)
		else if(content.equalsIgnoreCase("!giverole "))
			GiveRole.giveRole(auth, channel, guild, content, message);

		// Bot removes requested role from user (MODERATOR->PEASANT ONLY)
		else if(content.equalsIgnoreCase("!takerole "))
			TakeRole.takeRole(auth, channel, guild, content, message);

		// User requests to join/create an elective role
		else if(content.equalsIgnoreCase("!join "))
			Join.join(auth, author, channel, guild, content, conn);

		// Remove user's application from CSV file
		else if (content.equalsIgnoreCase("!leave "))
			Leave.leave(auth, author, channel, guild, content, conn);

		// Delete all non-specified roles (OWNER ONLY)
		else if (content.toLowerCase().equals("!cleanroles"))
			CleanRoles.cleanRoles(auth, channel, guild);

		// Delete all elective channels (OWNER ONLY)
		else if(content.equalsIgnoreCase("!cleanelectives"))
			CleanElectives.cleanElectives(auth, channel, guild);

		// Russian roulette
		else if (content.equalsIgnoreCase("!bang"))
			chamberCount = Roulette.roulette(author, chamberCount, channel, conn);

		// Russian roulette scores
		else if (content.equalsIgnoreCase("!bangscore") || content.equalsIgnoreCase("!bangscores"))
			BangScores.bangScores(channel, guild);

		// Show bang scores for individual
		else if (content.equalsIgnoreCase("!mybang"))
			MyBang.myBang(author, channel, conn);

		// Show available Elective roles
		else if (content.equalsIgnoreCase("!roles"))
			ShowRoles.showRoles(guild, channel);

		// Bet money for blackjack
		else if (content.toLowerCase().startsWith("!bet"))
			BlackJackCommands.bet(author, channel, content, conn);

		// Hit in blackjack
		else if (content.equalsIgnoreCase("!hit"))
			BlackJackCommands.hit(author, channel, conn);

		// Stand in blackjack
		else if (content.equalsIgnoreCase("!stand"))
			BlackJackCommands.stand(author, channel, conn);

		// Show hand in blackjack
		else if (content.equalsIgnoreCase("!hand"))
			BlackJackCommands.myHand(author, channel);

		// Show economy
		else if (content.equalsIgnoreCase("!wallet"))
			MyWallet.myWallet(author, channel, conn);

		/* Show listings
		else if (content.equalsIgnoreCase("!market"))
			market.showListings(channel);

		// Purchase listed colour
		else if (content.toLowerCase().startsWith("!purchase"))
			market.purchase(author, conn, content, channel);
			*/
	}
}

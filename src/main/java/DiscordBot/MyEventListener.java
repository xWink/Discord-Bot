package DiscordBot;

import DiscordBot.commands.admin_commands.*;
import DiscordBot.commands.bang.BangScores;
import DiscordBot.commands.bang.Daily;
import DiscordBot.commands.bang.MyBang;
import DiscordBot.commands.bang.Roulette;
import DiscordBot.commands.blackjack.BlackJackCommands;
import DiscordBot.commands.groups.Join;
import DiscordBot.commands.groups.Leave;
import DiscordBot.commands.groups.ShowRoles;
import DiscordBot.commands.misc.Help;
import DiscordBot.commands.misc.MyWallet;
import DiscordBot.commands.misc.Ping;
import DiscordBot.commands.tictactoe.ListOfWagers;
import DiscordBot.util.economy.Market;
import DiscordBot.util.tictactoe_util.Wager;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static DiscordBot.util.misc.DatabaseUtil.connect;

public class MyEventListener extends ListenerAdapter {

	private int chamberCount = 6;
	private ConfigFile cfg = RoleBot.config;

	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event){
		Guild guild = RoleBot.api.getGuildById(RoleBot.config.guildId);
		TextChannel generalChannel = guild.getTextChannelById("486633949154770946");
		TextChannel botsChannel = guild.getTextChannelById("551828950871965696");

		if (generalChannel == null){
			System.out.println("Could not find general channel!");
			return;
		}

		generalChannel.sendMessage("Welcome " + event.getUser().getAsMention() +
				"! Feel free to ask any questions in here, we are always looking to help each other out!\n\n" +
				"If you want to play with our bot, made in-house, go to " + botsChannel.getAsMention() +
				" and say `!help` :smiley:").queue();
	}

	@Override
	public void onDisconnect(DisconnectEvent event){}
	
  	@Override
  	public void onMessageReceived(MessageReceivedEvent event){

		final User author = event.getAuthor(); // Author as type User

		// If the event is made by the bot, ignore it
		if (author.isBot())
			return;

		final Guild guild = RoleBot.api.getGuildById(Long.parseLong(RoleBot.config.guildId));
		if (guild == null){
			System.out.println("Could not get guild from id, terminating event");
			return;
		}

		final Message message = event.getMessage(); // Detected message
		final String content = message.getContentRaw().trim(); // Text of the message
		final TextChannel channel = event.getTextChannel(); // Text channel the message came from
		final Member auth = guild.getMember(author); // Author as type Member
	  	final List channels = Arrays.asList(cfg.channel); // List of channels bot can read from
	  	final Market market = new Market(guild); // Roles for sale
	  	Connection conn;

		// Check if the bot is allowed to send messages in the current channel
		if (!cfg.channel[0].equalsIgnoreCase("all") && !channels.contains(channel.getId())) return;

		// Connect to database
		if ((conn = connect()) == null){
			channel.sendMessage("Could not connect to database. Please contact a moderator :(").complete();
			return;
		}

		// Bot shows how to use its commands
		if (content.equalsIgnoreCase("!help"))
			Help.help(channel);

		// Bot responds with pong and latency
		else if (content.equalsIgnoreCase("!ping"))
			Ping.ping(author, event, channel, conn);

		// Bot creates new text channel and deletes old one (OWNER ONLY)
		else if (content.equalsIgnoreCase("!totalchatwipe"))
			TotalChatWipe.chatWipe(auth, guild, channel);

		// Bot gives requested role to target (MODERATOR->PEASANT ONLY)
		else if(content.toLowerCase().startsWith("!giverole "))
			GiveRole.giveRole(auth, channel, guild, content, message);

		// Bot removes requested role from user (MODERATOR->PEASANT ONLY)
		else if(content.toLowerCase().startsWith("!takerole "))
			TakeRole.takeRole(auth, channel, guild, content, message);

		// User requests to join/create an elective role
		else if(content.toLowerCase().startsWith("!join"))
			Join.join(auth, author, channel, guild, content, conn);

		// Remove user's application from database and removes them from the role
		else if (content.toLowerCase().startsWith("!leave"))
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
			BangScores.bangScores(channel, guild, conn);

		// Show bang scores for individual
		else if (content.equalsIgnoreCase("!mybang"))
			MyBang.myBang(author, channel, conn);

		// Show daily reset time for bang
		else if (content.equalsIgnoreCase("!daily"))
			Daily.daily(author, conn, channel);

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

		// Show listings
		else if (content.equalsIgnoreCase("!market"))
			market.showListings(channel);

		// Purchase listed colour
		else if (content.toLowerCase().startsWith("!buy")){
			try{
				market.purchase(author, conn, content, channel);
			}
			catch (SQLException e){
				e.printStackTrace();
			}
		}

		// Create TicTacToe wager
		else if (content.toLowerCase().startsWith("!wager")){
			Wager wager = new Wager(channel, message, content, conn);
			channel.sendMessage("Wager amount: " + wager.getWagerAmount()).complete();
		}
	}
}

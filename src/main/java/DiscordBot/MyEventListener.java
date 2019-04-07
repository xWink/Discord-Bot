package DiscordBot;

/*
Copyright 2019 Shawn Kaplan

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
(the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import DiscordBot.commands.*;

public class MyEventListener extends ListenerAdapter {

	int chamberCount = 6;

	@Override
	public void onMessageReceived(MessageReceivedEvent event){

		User author = event.getAuthor(); // Variable author is the author of type User
		if (author.isBot()) return; // If the event is made by the bot, ignore it

		Message message = event.getMessage(); // Variable message is the detected message
		String content = message.getContentRaw(); // Variable content is the text of the message
		MessageChannel channel = event.getChannel(); // Variable channel is the text channel the message came from
		Guild guild = event.getGuild(); // Variable guild is the Discord server
		Member auth = guild.getMember(author); // Variable auth is author of type Member
		//String path = "C:\\Users\\User\\IdeaProjects\\Java\\src\\DiscordBots\\src\\main\\java\\ElectiveRequests.csv";
		//String path2 = "C:\\Users\\User\\IdeaProjects\\Java\\src\\DiscordBots\\src\\main\\java\\ScoreList.csv";
		String path = "/home/botadmin/ElectiveRequests.csv"; // applicant file path
		String path2 = "/home/botadmin/ScoreList.csv"; // score file path
		String path3 = "/home/botadmin/RouletteData.csv"; // roulette file path


		// Bot shows how to use !join
		if (content.toLowerCase().equals("!join") || content.toLowerCase().equals("!join ")){
			channel.sendMessage("Command: !join <courseID>\n\nExample: !join mcs2100").queue();
		}

		// Bot shows how to use !leave
		else if (content.toLowerCase().equals("!leave") || content.toLowerCase().equals("!leave ")){
			channel.sendMessage("Command: !leave <courseID>\n\nExample: !leave mcs2100").queue();
		}

		// Bot shows how to use !join and !leave
		else if (content.toLowerCase().equals("!help")){
			channel.sendMessage("For instructions to join an elective group, say \"!join\" and to leave one, say \"!leave\"").queue();
		}

		// Bot responds with pong and latency
		else if (content.toLowerCase().equals("!ping")) {
			Ping.ping(author, event, channel ,path2);
		}

		// Bot creates new text channel and deletes old one (OWNER ONLY)
		else if (content.toLowerCase().equals("!totalchatwipe")) {
			TotalChatWipe.chatWipe(auth, guild, channel);
		}

		// Bot gives requested role to target (MODERATOR->PEASANT ONLY)
		else if(content.toLowerCase().startsWith("!giverole ")){
			GiveRole.giveRole(auth, channel, guild, content, message);
		}

		// Bot removes requested role from user (MODERATOR->PEASANT ONLY)
		else if(content.startsWith("!takerole ")) {
			TakeRole.takeRole(auth, channel, guild, content, message);
		}

		// User requests to join/create an elective role (EVERYONE)
		else if(content.startsWith("!join ")){
			Join.join(auth, author, channel, guild, content, path);
		}

		// Remove user's application from CSV file
		else if (content.toLowerCase().startsWith("!leave ")){
			Leave.leave(auth, author, channel, guild, content, path);
		}

		// Delete all non-specified roles (OWNER ONLY)
		else if (content.toLowerCase().equals("!cleanroles")){
			CleanRoles.cleanRoles(auth, channel, guild);
		}

		// Delete all elective channels (OWNER ONLY)
		else if(content.toLowerCase().equals("!cleanelectives")){
			CleanElectives.cleanElectives(auth, channel, guild);
		}

		// Show score
		else if (content.toLowerCase().equals("!score") || content.toLowerCase().equals("!scores")){
			Scores.scores(channel, path2);
		}

		// Russian roulette
		else if (content.toLowerCase().equals("!bang")) {
			chamberCount = Roulette.roulette(author, path3, chamberCount,  channel);
			System.out.println(chamberCount+" Success!\n");
		}
	}
}

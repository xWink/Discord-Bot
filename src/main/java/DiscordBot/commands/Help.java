package DiscordBot.commands;

import net.dv8tion.jda.core.entities.MessageChannel;

public class Help {

    public static void help(MessageChannel channel){

        channel.sendMessage("For instructions to join an elective group, say \"!join\" and to leave one, say \"!leave\"").queue();
        channel.sendMessage("To play Russian Roulette, say \"!bang\" or to use Ping, say \"!ping\"").queue();
        channel.sendMessage("To see the top Ping scores, say \"!scores\"").queue();
    }
}

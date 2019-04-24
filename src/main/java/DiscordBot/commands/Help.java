package DiscordBot.commands;

import net.dv8tion.jda.core.entities.MessageChannel;

public class Help {

    public static void help(MessageChannel channel){

        channel.sendMessage("For instructions to join an elective group, say \"!join\" and to leave one, say \"!leave\". To see available channels, say \"!roles\"").queue();
        channel.sendMessage("To play Russian Roulette, say \"!bang\" or to use Ping, say \"!ping\"").queue();
        channel.sendMessage("To see roulette high-scores, say \"!bangscores\", to see personal roulette scores, say \"!mybang\" or to see Ping scores, say \"!scores\"").queue();
    }
}

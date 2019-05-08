package DiscordBot.commands;

import DiscordBot.helpers.*;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import DiscordBot.helpers.BangHighScores;

public class BangScores {

    public static void bangScores(MessageChannel channel, Guild guild){

        BangHighScores highScores = GetBangScores.getBangScores(guild);

        if (highScores != null){
            channel.sendMessage("The player with the most attempts is "+highScores.mostAttemptsPlayer+" with a score of "+(int)highScores.attemptCount).queue();
            channel.sendMessage("The player with the most deaths is "+highScores.mostDeathsPlayer+" with a score of "+(int)highScores.deathCount).queue();
            channel.sendMessage("The player with the best survival rate is "+highScores.luckiest+" at "+highScores.bestRate+"%").queue();
            channel.sendMessage("The player with the worst survival rate is "+highScores.unluckiest+" at "+highScores.worstRate+"%").queue();
        }
    }
}

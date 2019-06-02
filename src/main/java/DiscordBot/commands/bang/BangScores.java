package DiscordBot.commands.bang;

import DiscordBot.util.bang_util.BangHighScores;
import DiscordBot.util.bang_util.GetBangScores;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

public class BangScores {

    public static void bangScores(TextChannel channel, Guild guild){

        BangHighScores highScores = GetBangScores.getBangScores(guild);

        if (highScores != null){
            channel.sendMessage("The player with the most attempts is "+highScores.getMostAttemptsPlayer().getName()+" with a score of "+(int)highScores.getAttemptCount()+"\n"+
            "The player with the most deaths is "+highScores.getMostDeathsPlayer().getName()+" with a score of "+(int)highScores.getDeathCount()+"\n"+
            "The player with the best survival rate is "+highScores.getLuckiest().getName()+" at "+highScores.getBestRate()+"%\n"+
            "The player with the worst survival rate is "+highScores.getUnluckiest().getName()+" at "+highScores.getWorstRate()+"%\n"+
            "The player with the most jams survived is "+highScores.getMostJamsPlayer().getName()+" at "+highScores.getJamCount()).queue();
        }
    }
}

package DiscordBot.commands.bang;

import DiscordBot.util.bang_util.BangHighScores;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

import java.sql.Connection;

import static DiscordBot.util.bang_util.GetBangScores.*;

public class BangScores {

    public static void bangScores(TextChannel channel, Guild guild, Connection conn){

        BangHighScores highScores = getHighScores(guild);
        String string = "";

        // Make string with high scores
        if (highScores != null){
            string = "Most attempts: "+highScores.getMostAttemptsPlayer().getName()+" with a score of "+(int)highScores.getAttemptCount()+"\n"+
            "Best survival rate: "+highScores.getLuckiest().getName()+" at "+highScores.getBestRate()+"%\n"+
            "Worst survival rate: "+highScores.getUnluckiest().getName()+" at "+highScores.getWorstRate()+"%\n"+
            "Wealthiest player: "+highScores.getWealthiest().getName()+" with "+highScores.getWealth()+" gc\n\n";
        }


        // Concat string with total attempts/deaths
        int[] totals = getTotalBangs(conn);
        string = string.concat("Total attempts: " + totals[0] + "\nTotal deaths: " + totals[1]);

        // Send message
        channel.sendMessage(string).complete();
    }
}

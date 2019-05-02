package DiscordBot.helpers;

import net.dv8tion.jda.core.entities.Guild;
import src.main.java.DiscordBot.helpers.BangHighScores;

public class UpdateBangRoles {

    public static void updateBangRoles(){

        final String path3 = "/home/botadmin/RouletteData.csv"; // roulette file path
        Guild guild = DiscordBot.MyEventListener.guild;
        BangHighScores highScores = DiscordBot.helpers.GetBangScores.getBangScores(path3,guild);

        if (highScores != null) {
            int size = guild.getMembersByName(highScores.luckiest, true).size();
            System.out.println("\n"+size+"\n");
        }
    }
}

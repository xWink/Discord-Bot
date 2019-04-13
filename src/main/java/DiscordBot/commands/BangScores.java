package DiscordBot.commands;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BangScores {

    public static void bangScores(MessageChannel channel, String path3, Guild guild){
        System.out.println("Test");
        try{
            Path scorePath = Paths.get(path3);
            BufferedReader reader = Files.newBufferedReader(scorePath);
            BufferedReader bufferedReader = Files.newBufferedReader(scorePath);
            String line;
            int lineCount = 0, i = 0, mostAttempts = -1,  mostDeaths = -1, luckiest = -1, unluckiest = -1;
            double bestRate = -1.00, worstRate = 100.00, attemptCount = 0, deathCount = 0;

            // Get number of lines
            while ((line = bufferedReader.readLine()) != null){
                if (line.startsWith("\"")){
                    lineCount++;
                }
            }
            bufferedReader.close();

            BangPlayer playerArray[] = new BangPlayer[lineCount];
            System.out.println("Test");
            // Store file content in array of BangPlayers
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("\"")){
                    playerArray[i] = new BangPlayer(
                            guild.getMemberById(line.substring(line.indexOf("\"") + 1, line.indexOf("\",\""))).getUser().getName(),
                            (double)Integer.parseInt(line.substring(line.indexOf("\",\"") + 3, line.indexOf("\"", line.indexOf("\",\"") + 3))),
                            (double)Integer.parseInt(line.substring(line.indexOf("\",\"", line.indexOf("\",\"") + 3) + 3, line.length() - 1))
                    );
                    i++;
                }
            }
            reader.close();

            // Find relevant scores
            for (i = 0; i < lineCount; i++){
                double attempts = playerArray[i].attempts;
                double deaths = playerArray[i].deaths;
                System.out.println("Attempts: "+attempts+" from "+playerArray[i].player);
                System.out.println("Deaths: "+deaths+" from "+playerArray[i].player);

                // Most attempts
                if (attempts > attemptCount){
                    mostAttempts = i;
                    attemptCount = attempts;
                }
                // Most deaths
                if (deaths > deathCount){
                    mostDeaths = i;
                    deathCount = deaths;
                }
                // Luckiest
                if (attempts >= 20 && 100 - (deaths / attempts * 100) > bestRate){
                    luckiest = i;
                    bestRate = 100 - (deaths / attempts * 100);
                    System.out.println("Best rate: "+bestRate);
                    System.out.println("Luckiest: "+playerArray[i].player);
                }
                // Unluckiest
                if (attempts >= 20 && 100 - (deaths / attempts * 100) < worstRate){
                    unluckiest = i;
                    worstRate = 100 - (deaths / attempts * 100);
                    System.out.println("Worst rate: "+worstRate);
                    System.out.println("Unluckiest: "+playerArray[i].player);
                }
            }
            bestRate = Math.round(bestRate * 10) / 10;
            worstRate = Math.round(worstRate * 10) / 10;
            System.out.println("Best rate final: "+bestRate);
            System.out.println("Worst rate final: "+worstRate);

            // Print score messages
            if (mostAttempts > -1)
                channel.sendMessage("The player with the most attempts is "+playerArray[mostAttempts].player+" with a score of "+attemptCount).queue();
            if (mostDeaths > -1)
                channel.sendMessage("The player with the most deaths is "+playerArray[mostDeaths].player+" with a score of "+deathCount).queue();
            if (luckiest > -1)
                channel.sendMessage("The player with the best survival rate is "+playerArray[luckiest].player+" at "+bestRate+"%").queue();
            if (unluckiest > -1)
                channel.sendMessage("The player with the worst survival rate is "+playerArray[unluckiest].player+" at "+worstRate+"%").queue();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}

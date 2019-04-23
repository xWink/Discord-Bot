package DiscordBot.commands;

import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MyBang {

    public static void myBang(User author, MessageChannel channel, String path3){

        try {
            Path scorePath = Paths.get(path3);
            BufferedReader reader = Files.newBufferedReader(scorePath);
            String line;
            BangPlayer player = null;
            int lineCount = 0;

            // Find player in scores file
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("\""+author.getId()+"\"")) {
                    player = new BangPlayer(
                            author.getName(),
                            (double)Integer.parseInt(line.substring(line.indexOf("\",\"") + 3, line.indexOf("\"", line.indexOf("\",\"") + 3))),
                            (double)Integer.parseInt(line.substring(line.indexOf("\",\"", line.indexOf("\",\"") + 3) + 3, line.length() - 1)));
                }
            }
            reader.close();

            // Post results
            if (player != null){
                double survivalRate = player.deaths / player.attempts;
                channel.sendMessage("Scores for"+author.getName()+":\nAttempts: "+player.attempts+"\nDeaths: "+player.deaths+"\nSurvival rate: "+survivalRate).queue();
            }else{
                channel.sendMessage(author.getName()+" could not be found in score list").queue();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}

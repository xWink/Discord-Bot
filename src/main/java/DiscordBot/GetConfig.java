package DiscordBot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

import java.io.File;
import java.util.Scanner;

public class GetConfig {
    public static void getConfig() {
        ArrayList<String> rawFile = new ArrayList<String>();
        File file = new File(".rolebotconfig");
        Scanner readConfig = new Scanner (file);

        // Read all of the contents of the file and print to screen
        while (readConfig.hasNext()) {
            rawFile.add(readConfig.next())
        }

    }
}
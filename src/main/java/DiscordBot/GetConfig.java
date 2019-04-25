package DiscordBot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

import java.io.File;
import java.util.Scanner;
import java.util.ArrayList;

import DiscordBot.ConfigFile;

public class GetConfig {
    public static void logMessage (String m) {
        System.out.println(m);
    }

    public static String parseArg (String currLn) {
        String parsedLn = new String (currLn.substring(currLn.indexOf('=') + 1));
        return parsedLn;
    }

    public static ConfigFile getConfig() throws Exception {
        ArrayList<String> rawFile = new ArrayList<String>();
        ArrayList<String> rawChannels = new ArrayList<String>();
        int current = 0;
        ConfigFile toReturn = new ConfigFile();

        // Set up file reading
        File file = new File(".rolebotconfig");
        Scanner readConfig = new Scanner (file);
        logMessage ("Reading config file...");

        // Read the file
        try {
            while (readConfig.hasNext()) {
                rawFile.add(readConfig.next());
                current++;
            }
            readConfig.close();
        }
        // If file cannot be found
        catch (Exception e) {
            logMessage ("Error! File '.rolebotconfig' could not be found");
            System.exit(0);
        }

        // Place raw file contents into String array for easier manipulation
        String[] contents = rawFile.toArray(new String[0]);

        // Parse through the rawContents array and get the important information
        for (int i = 0; i < current; i++) {
            // Set current line
            String currLn = new String(contents[i]);

            // If the line is a token line
            if (currLn.startsWith("TOKEN")) {
                toReturn.token = new String (parseArg (currLn));
                logMessage ("Got token [" + toReturn.token + "]");
            }

            // If the line is a channel line, add to rawChannels ArrayList
            else if (currLn.startsWith("CHANNEL")) {
                String tempChannel = new String (parseArg (currLn));
                rawChannels.add(tempChannel); 
                logMessage ("Got channel [" + tempChannel + "]");
            }

            // If the line is an applicant path line
            else if (currLn.startsWith("APPLICANT_PATH")) {
                toReturn.applicant_path = new String (parseArg (currLn));
                logMessage ("Got applicant file path [" + toReturn.applicant_path + "]");
            }

            // If the line is a score path line
            else if (currLn.startsWith("SCORE_PATH")) {
                toReturn.score_path = new String (parseArg (currLn));
                logMessage ("Got score file path [" + toReturn.score_path + "]");
            }

            // If the line is a roulette path
            else if (currLn.startsWith("ROULETTE_PATH")) {
                toReturn.roulette_path = new String (parseArg (currLn));
                logMessage ("Got roulette file path [" + toReturn.roulette_path + "]");
            }
        }

        // Place the contents of rawChannels into ConfigFile object
        toReturn.channel = rawChannels.toArray (new String[0]);

        // Ensure that all fields are filled
        if ( (toReturn.token == null) || (toReturn.token.isEmpty()) ) {
            logMessage ("Error: Ending execution due to missing TOKEN in '.rolebotconfig' file. Make sure that the file has a TOKEN field before running again");
            System.exit(0);
        }
        else if ( (toReturn.channel == null) || (toReturn.channel[0].isEmpty()) ) {
            logMessage ("Error: Ending execution due to missing CHANNEL in '.rolebotconfig' file. Make sure that the file has at least one CHANNEL field before running again");
            logMessage ("If you would like the bot to work on all channels, use `CHANNEL=all`");
            System.exit(0);
        }
        else if ( (toReturn.applicant_path == null) || (toReturn.applicant_path.isEmpty()) ) {
            logMessage ("Error: Ending execution due to missing APPLICANT_PATH in '.rolebotconfig' file. Make sure that the file has an APPLICANT_PATH field before running again");
            System.exit(0);
        }
        else if ( (toReturn.score_path == null) || (toReturn.score_path.isEmpty()) ) {
            logMessage ("Error: Ending execution due to missing SCORE_PATH in '.rolebotconfig' file. Make sure that the file has a SCORE_PATH field before running again");
            System.exit(0);
        }
        else if ( (toReturn.roulette_path == null) || (toReturn.roulette_path.isEmpty()) ) {
            logMessage ("Error: Ending execution due to missing ROULETTE_PATH in '.rolebotconfig' file. Make sure that the file has a ROULETTE_PATH field before running again");
            System.exit(0);
        }

        return toReturn;
    } // end getConfig()

}
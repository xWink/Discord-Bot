package DiscordBot;

import java.io.File;
import java.util.Scanner;
import java.util.ArrayList;

class GetConfig {

    private static void logMessage (String m) {
        System.out.println(m);
    }

    private  static String parseArg (String currLn) {
        return currLn.substring(currLn.indexOf('=') + 1);
    }

    static ConfigFile getConfig() throws Exception {

        final String configFilename = ".rolebotconfig";
        ArrayList<String> rawFile = new ArrayList<>();
        ArrayList<String> rawChannels = new ArrayList<>();
        ConfigFile toReturn = new ConfigFile();
        File working = new File (configFilename);
        File home = new File (System.getProperty("user.home") + configFilename);
        Scanner readConfig = null;
        int current = 0;

        // Check whether config file exists in working (current) directory
        if (working.exists()) {
            logMessage ("Found [" + configFilename + "] in current directory");
            readConfig = new Scanner (working);
        }

        // Check if file is in home directory
        else if (home.exists()) {
            logMessage ("Found [" + configFilename + "] in home directory");
            readConfig = new Scanner (home);
        }

        // If the file cannot be found
        if (readConfig == null) {
            logMessage("Error: Ending execution due to missing `" + configFilename + "` file.");
            logMessage("Place the file in the same directory as the .jar file or in the home directory and run the bot again");
            System.exit(0);
        }

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
            // If the line is a token line
            if (contents[i].startsWith("TOKEN")) {
                toReturn.token = parseArg(contents[i]);
                logMessage ("Got token [" + toReturn.token + "]");
            }

            // If the line is a channel line, add to rawChannels ArrayList
            else if (contents[i].startsWith("CHANNEL")) {
                rawChannels.add(parseArg(contents[i]));
                logMessage ("Got channel [" + parseArg(contents[i]) + "]");
            }

            // If the line is an applicant path line
            else if (contents[i].startsWith("APPLICANT_PATH")) {
                toReturn.applicant_path = parseArg(contents[i]);
                logMessage ("Got applicant file path [" + toReturn.applicant_path + "]");
            }

            // If the line is a score path line
            else if (contents[i].startsWith("SCORE_PATH")) {
                toReturn.score_path = parseArg(contents[i]);
                logMessage ("Got score file path [" + toReturn.score_path + "]");
            }

            // If the line is a roulette path
            else if (contents[i].startsWith("ROULETTE_PATH")) {
                toReturn.roulette_path = parseArg(contents[i]);
                logMessage ("Got roulette file path [" + toReturn.roulette_path + "]");
            }
        }

        // Place the contents of rawChannels into ConfigFile object
        toReturn.channel = rawChannels.toArray(new String[0]);

        // Ensure that all fields are filled
        if ( (toReturn.token == null) || (toReturn.token.isEmpty()) ) {
            logMessage ("Error: Ending execution due to missing TOKEN in '.rolebotconfig' file. Make sure that the file has a TOKEN field before running again");
            System.exit(0);
        }
        if ( (toReturn.channel[0] == null) || (toReturn.channel[0].isEmpty()) ) {
            logMessage ("Error: Ending execution due to missing CHANNEL in '.rolebotconfig' file. Make sure that the file has at least one CHANNEL field before running again");
            logMessage ("If you would like the bot to work on all channels, use `CHANNEL=all`");
            System.exit(0);
        }
        if ( (toReturn.applicant_path == null) || (toReturn.applicant_path.isEmpty()) ) {
            logMessage ("Error: Ending execution due to missing APPLICANT_PATH in '.rolebotconfig' file. Make sure that the file has an APPLICANT_PATH field before running again");
            System.exit(0);
        }
        if ( (toReturn.score_path == null) || (toReturn.score_path.isEmpty()) ) {
            logMessage ("Error: Ending execution due to missing SCORE_PATH in '.rolebotconfig' file. Make sure that the file has a SCORE_PATH field before running again");
            System.exit(0);
        }
        if ( (toReturn.roulette_path == null) || (toReturn.roulette_path.isEmpty()) ) {
            logMessage ("Error: Ending execution due to missing ROULETTE_PATH in '.rolebotconfig' file. Make sure that the file has a ROULETTE_PATH field before running again");
            System.exit(0);
        }

        return toReturn;
    }
}

package DiscordBot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

import java.io.File;
import java.util.Scanner;
import java.util.ArrayList;

import DiscordBot.ConfigFile;

public class GetConfig {
    public static String parseArg (String currLn) {
        String parsedLn = new String (currLn.substring(currLn.indexOf('=') + 1));
        return parsedLn;
    }

    public static String getConfig() throws Exception {
        ArrayList<String> rawFile = new ArrayList<String>();
        int current = 0;
        String token = "NTcwNzA1MTU0MjU3NzE1MjMx.XMDK3A.GOVN1ytQ7O4sn5ediRhRiML9s_A"; // remove later
        ConfigFile toReturn = new ConfigFile();

        // Set up file reading
        File file = new File(".rolebotconfig");
        Scanner readConfig = new Scanner (file);

        // Read the file
        try {
            while (readConfig.hasNext()) {
                rawFile.add(readConfig.next());
                System.out.println (rawFile.get(current));
                current++;
            }
            readConfig.close();
        }
        // If file cannot be found
        catch (Exception e) {
            System.out.println ("Error! File '.rolebotconfig' could not be found");
            System.exit(0);
        }

        // Place raw file contents into String array for easier manipulation
        String[] contents = rawFile.toArray(new String[0]);
        for (int i = 0; i < current; i++) {
            System.out.println (contents[i]);
        }

        // Parse through the rawContents array and get the important information
        for (int i = 0; i < current; i++) {
            // Set current line
            String currLn = new String(contents[i]);

            // Ignore comment lines (lines that start with #
            if (currLn.startsWith("#")) { break; }

            // If the line is a token line
            else if (currLn.startsWith("TOKEN")) {
                toReturn.token = new String (parseArg (currLn));
                System.out.println (toReturn.token);
            }

            // If the line is a channel line


            // If the line is an applicant path line
            else if (currLn.startsWith("APPLICANT_PATH")) {
                toReturn.applicant_path = new String (parseArg (currLn));
                System.out.println (toReturn.applicant_path);
            }

            // If the line is a score path line
            else if (currLn.startsWith("SCORE_PATH")) {
                toReturn.score_path = new String (parseArg (currLn));
                System.out.println (toReturn.score_path);
            }

            // If the line is a roulette path
            else if (currLn.startsWith("ROULETTE_PATH")) {
                toReturn.roulette_path = new String (parseArg (currLn));
                System.out.println (toReturn.roulette_path);
            }
        }

        return token;


    }


}
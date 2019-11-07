package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Config {

    private final static String configFilename = ".rolebotconfig";
    private static ArrayList<String> rawChannels;
    private static String token;
    private static String[] channels;
    private static String db_user;
    private static String db_pass;
    private static String guildId;


    static {
        rawChannels = new ArrayList<>();
        try {
            getConfig();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }


    private static void logMessage(String m) {
        System.out.println(m);
    }


    private static String parseArg(String currLn) {
        return currLn.substring(currLn.indexOf('=') + 1);
    }


    private static void getConfig() throws FileNotFoundException {
        ArrayList<String> rawFile = new ArrayList<>();
        File working = new File (configFilename);
        File home = new File (System.getProperty("user.home") + configFilename);
        Scanner readConfig = getScanner(working, home);
        int current = 0;

        // Read the file
        while (readConfig.hasNext()) {
            rawFile.add(readConfig.next());
            current++;
        }
        readConfig.close();

        // Place raw file contents into String array for easier manipulation
        String[] contents = rawFile.toArray(new String[0]);

        // Parse through the rawContents array and get the important information
        for (int i = 0; i < current; i++) {
            parseLine(contents[i]);
        }

        // Place the contents of rawChannels into ConfigFile object
        channels = rawChannels.toArray(new String[0]);

        // Ensure nothing is null
        checkForErrors();
    }


    private static Scanner getScanner(File working, File home) throws FileNotFoundException {
        // Check whether config file exists in working (current) directory
        if (working.exists()) {
            logMessage ("Found [" + configFilename + "] in current directory");
            return new Scanner(working);
        }
        // Check if file is in home directory
        else if (home.exists()) {
            logMessage ("Found [" + configFilename + "] in home directory");
            return new Scanner(home);
        }
        // If the file cannot be found
        logMessage("Error: Ending execution due to missing `" + configFilename + "` file.");
        logMessage("Place the file in the same directory as the .jar file or in the home directory and run the bot again");
        System.exit(0);
        return null;
    }


    static String getToken() {
        return token;
    }

    public static String getDb_user() {
        return db_user;
    }

    public static String getDb_pass() {
        return db_pass;
    }


    private static void parseLine(String contents) {
        // If the line is a token line
        if (contents.startsWith("TOKEN")) {
            token = parseArg(contents);
            logMessage ("Got token");
        }
        // If the line is a channel line, add to rawChannels ArrayList
        else if (contents.startsWith("CHANNEL")) {
            rawChannels.add(parseArg(contents));
            logMessage ("Got channel [" + parseArg(contents) + "]");
        }
        // If the line is a database username
        else if (contents.startsWith("DB_USER")){
            db_user = parseArg(contents);
            logMessage("Got database username");
        }
        // If the line is a database password
        else if (contents.startsWith("DB_PASS")){
            db_pass = parseArg(contents);
            logMessage("Got database password");
        }
        // If the line is the guild ID
        else if (contents.startsWith("GUILD")){
            guildId = parseArg(contents);
            logMessage("Got guild ID");
        }
    }


    private static void checkForErrors() {
        // Ensure that all fields are filled
        if ((token == null) || (token.isEmpty())) {
            logMessage ("Error: Ending execution due to missing TOKEN in '.rolebotconfig' file. Make sure that the file has a TOKEN field before running again");
            System.exit(0);
        }
        if ((channels[0] == null) || (channels[0].isEmpty())) {
            logMessage ("Error: Ending execution due to missing CHANNEL in '.rolebotconfig' file. Make sure that the file has at least one CHANNEL field before running again");
            logMessage ("If you would like the bot to work on all channels, use `CHANNEL=all`");
            System.exit(0);
        }
        if ((db_user == null) || (db_user.isEmpty())) {
            logMessage ("Error: Ending execution due to missing DB_USER in '.rolebotconfig' file. Make sure that the file has a DB_USER field before running again");
            System.exit(0);
        }
        if ((db_pass == null) || (db_pass.isEmpty())) {
            logMessage ("Error: Ending execution due to missing DB_PASS in '.rolebotconfig' file. Make sure that the file has a DB_PASS field before running again");
            System.exit(0);
        }
        if (guildId == null || guildId.isEmpty()) {
            logMessage ("Error: Ending execution due to missing GUILD in '.rolebotconfig' file. Make sure that the file has a GUILD field before running again");
            System.exit(0);
        }
    }
}

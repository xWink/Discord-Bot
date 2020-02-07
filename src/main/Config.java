package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public final class Config {

    private static ArrayList<String> rawChannels;
    private static String token;
    private static String[] channels;
    private static String dbAddr;
    private static String dbUser;
    private static String dbPass;
    private static String guildId;
    private static final String CONFIG_FILENAME = ".rolebotconfig";

    private Config() {

    }

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
        File working = new File(CONFIG_FILENAME);
        File home = new File(System.getProperty("user.home") + CONFIG_FILENAME);
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
            logMessage("Found [" + CONFIG_FILENAME + "] in current directory");
            return new Scanner(working);
        } else if (home.exists()) { // Check if file is in home directory
            logMessage("Found [" + CONFIG_FILENAME + "] in home directory");
            return new Scanner(home);
        }
        // If the file cannot be found
        logMessage("Error: Ending execution due to missing `" + CONFIG_FILENAME + "` file.");
        logMessage("Place the file in the same directory as the .jar file or in the home directory and run the bot again");
        System.exit(0);
        return null;
    }


    static String getToken() {
        return token;
    }

    /**
     * Database address getter.
     * @return The address to connect to the database.
     */
    public static String getDbAddr() {
        return dbAddr;
    }

    /**
     * Database username getter.
     * @return the database's username for login
     */
    public static String getDbUser() {
        return dbUser;
    }

    /**
     * Database password getter.
     * @return the database's password for login
     */
    public static String getDbPass() {
        return dbPass;
    }

    /**
     * Guild ID getter.
     * @return the ID of the guild to connect to
     */
    public static String getGuildId() {
        return guildId;
    }

    /**
     * Channels getter.
     * @return the channels used for the bot's non-global commands
     */
    public static String[] getChannels() {
        return channels;
    }


    private static void parseLine(String contents) {
        if (contents.startsWith("TOKEN")) { // If the line is a token line
            token = parseArg(contents);
            logMessage("Got token");
        } else if (contents.startsWith("CHANNEL")) { // If the line is a channel line, add to rawChannels ArrayList
            rawChannels.add(parseArg(contents));
            logMessage("Got channel [" + parseArg(contents) + "]");
        } else if (contents.startsWith("DB_ADDR")) {
            dbAddr = parseArg(contents);
            logMessage("Got database address");
        } else if (contents.startsWith("DB_USER")) { // If the line is a database username
            dbUser = parseArg(contents);
            logMessage("Got database username");
        } else if (contents.startsWith("DB_PASS")) { // If the line is a database password
            dbPass = parseArg(contents);
            logMessage("Got database password");
        } else if (contents.startsWith("GUILD")) { // If the line is the guild ID
            guildId = parseArg(contents);
            logMessage("Got guild ID");
        }
    }


    private static void checkForErrors() {
        // Ensure that all fields are filled
        if ((token == null) || (token.isEmpty())) {
            logMessage("Error: Ending execution due to missing TOKEN in '.rolebotconfig' file. Make sure that the file has a TOKEN field before running again");
            System.exit(0);
        }
        if ((channels[0] == null) || (channels[0].isEmpty())) {
            logMessage("Error: Ending execution due to missing CHANNEL in '.rolebotconfig' file. Make sure that the file has at least one CHANNEL field before running again");
            logMessage("If you would like the bot to work on all channels, use `CHANNEL=all`");
            System.exit(0);
        }
        if ((dbUser == null) || (dbUser.isEmpty())) {
            logMessage("Error: Ending execution due to missing DB_USER in '.rolebotconfig' file. Make sure that the file has a DB_USER field before running again");
            System.exit(0);
        }
        if ((dbPass == null) || (dbPass.isEmpty())) {
            logMessage("Error: Ending execution due to missing DB_PASS in '.rolebotconfig' file. Make sure that the file has a DB_PASS field before running again");
            System.exit(0);
        }
        if (guildId == null || guildId.isEmpty()) {
            logMessage("Error: Ending execution due to missing GUILD in '.rolebotconfig' file. Make sure that the file has a GUILD field before running again");
            System.exit(0);
        }
    }
}

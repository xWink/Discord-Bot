/*
 * Made by Jeremy Thorne.
 * GitHub: https://github.com/jeremyt123
 */
package command.commands.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import command.Command;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Info extends Command {

    private BufferedReader reader;
    private String courseId;

    /**
     * Sets up the file reader for the tsv.
     */
    public Info() {
        super("!info", true);
    }

    /**
     * Closes the reader.
     */
    private void closeFile() {
        try {
            reader.close();
        } catch (IOException e) {
            System.out.println("Error closing file");
        }
    }

    /**
     * Searches the file for the course.
     *
     * @return a String with all the course info
     */
    private String searchCourse() {
        String line, temp;
        try {
            while ((line = reader.readLine()) != null) {
                temp = line.split("\t")[0];
                if (temp.replace("*", "").toLowerCase()
                        .contains(courseId.replace("*", "").toLowerCase())) {
                    return line.replaceAll("\"", "");
                }
            }
        } catch (IOException ignored) { }

        return "Could not find " + courseId;
    }

    /**
     * Prints nicely formatted course info.
     *
     * @param line String containing ts course info
     * @return String containing info to print
     */
    private String printNice(String line) {
        if ((line.chars().filter(c -> c == '\t').count()) != 2) {
            return line;
        }
        String out = "";

        String name = line.split("\t")[0];
        String restrict = line.split("\t")[1];
        String prereq = line.split("\t")[2];

        out = out.concat("**" + name + "**\n```");

        if (restrict.equals("NONE")) {
            out += "Restriction(s): None\n\n";
        } else {
            out += (restrict + "\n\n");
        }

        if (prereq.equals("NONE")) {
            out += "Prerequisite(s): None";
        } else {
            out += prereq;
        }

        out += "```";
        return out;
    }

    /**
     * Key matches if the string equals exactly (ignoring case) the key.
     *
     * @param string the user's input being compared to the key
     * @return true if the key matches the string
     */
    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().matches("^" + getKey() + " .+$");
    }

    /**
     * Sets up the couse searching and outputs the course it finds.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(final MessageReceivedEvent event) {
        String[] strings = event.getMessage().getContentRaw().split(" ");

        if (strings.length < 2) {
            event.getChannel().sendMessage("To search for course info, say `!info <course ID>`").queue();
            return;
        }

        courseId = strings[1];
        try {
            String tsv = new File("").getAbsolutePath();
            tsv = tsv.replace("build/libs", "") + "res/courses.tsv";
            reader = new BufferedReader(new FileReader(tsv));
            event.getChannel().sendMessage(printNice(searchCourse())).queue();
        } catch (FileNotFoundException e) {
            printStackTraceAndSendMessage(event, e);
        } finally {
            closeFile();
        }
    }
}

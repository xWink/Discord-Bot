package command;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Info extends Command {
    BufferedReader reader;
    String code;
    
    /**
     * Sets up the file reader for the tsv.
     */
    Info() {
        super("!info");
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
     * @param courseID course code
     * @return a String with all the course info
     */
    private String searchCourse(String courseID) {
        String line, temp;
        try {
            while ((line = reader.readLine()) != null) {
                temp = line.split("\t")[0];
                if (temp.replace("*","").toLowerCase().contains(courseID.replace("*", "").toLowerCase())) {
                    return line.replaceAll("\"", ""); 
                }
            }            
        } catch (IOException e) {
            return "Course not found";
        } 
    }

    /**
     * Prints nicely formatted course info.
     * 
     * @param line String containing ts course info
     * @return String containing info to print
     */
    private String printNice(String line) {
        if ((line.chars().filter(c -> c == '\t').count()) != 2) {
            System.out.println("Course not found");
            return "";
        }
        StringBuilder out = new StringBuilder();

        String name = line.split("\t")[0];
        String restrict = line.split("\t")[1];
        String prereq = line.split("\t")[2];

        out.append(name + "\n");

        if (restrict.equals("NONE")) {
            out.append("Restriction(s): None\n");
        } else {
            out.append(restrict + "\n");
        }

        if (prereq.equals("NONE")) {
            out.append("Prerequisite(s): None");
        } else {
            out.append(prereq);
        }
        return out.toString();
    }

    /**
     * Key matches if the string equals exactly (ignoring case) the key.
     *
     * @param string the user's input being compared to the key
     * @return true if the key matches the string
     */
    @Override
    public boolean keyMatches(String string) {
        code = string.split(" ")[1];
        return string.equalsIgnoreCase(getKey());
    }

    /**
     * Sets up the couse searching and outputs the course it finds.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(final MessageReceivedEvent event) {
        try {
            reader = new BufferedReader(new FileReader("../res/courses.tsv"));
            event.getChannel().sendMessage(printNice(searchCourse(code))).queue();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            event.getChannel().sendMessage("An error has occured, please contact a moderator");
        } finally {
            closeFile();
        }
    }
}

package command;

import java.util.ArrayList;

/**
 * Container class for a list of all the commands
 * available in the server.
 */
public class CommandList {

    private static ArrayList<Command> commands;


    static {
        commands = new ArrayList<>();
        addAllCommands();
    }


    /**
     * Commands list getter.
     *
     * @return the ArrayList of all commands
     */
    public static ArrayList<Command> getCommands() {
        return commands;
    }


    /**
     * Populates the commands list with every command.
     */
    private static void addAllCommands() {
        commands.add(new Ping());
        commands.add(new Help());
        commands.add(new Karma());
    }
}

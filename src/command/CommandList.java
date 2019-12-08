package command;

import command.commands.economy.Buy;
import command.commands.bang.Daily;
import command.commands.Help;
import command.commands.Info;
import command.commands.Karma;
import command.commands.economy.Market;
import command.commands.bang.MyBang;
import command.commands.Ping;
import command.commands.Purge;
import command.commands.economy.Wallet;

import java.util.ArrayList;

/**
 * Container class for a list of all the commands
 * available in the server.
 */
public final class CommandList {

    private static ArrayList<Command> commands;

    private CommandList() {

    }

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
    private static void addAllCommands() { // TODO: Add Bang when it's done
        // Admin commands
        commands.add(new Purge());
        // Everyone commands
        commands.add(new Ping());
        commands.add(new Help());
        commands.add(new Karma());
        commands.add(new Daily());
        commands.add(new MyBang());
        commands.add(new Wallet());
        commands.add(new Info());
        commands.add(new Market());
        commands.add(new Buy());
    }
}

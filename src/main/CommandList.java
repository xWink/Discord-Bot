package main;

import command.Command;
import command.commands.admin.*;
import command.commands.bang.Bang;
import command.commands.bang.BangScore;
import command.commands.blackjack.Bet;
import command.commands.blackjack.Hit;
import command.commands.blackjack.MyHand;
import command.commands.blackjack.Stand;
import command.commands.economy.Buy;
import command.commands.Help;
import command.commands.economy.Gift;
import command.commands.economy.Market;
import command.commands.misc.*;
import command.commands.bang.Daily;

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
    private static void addAllCommands() {
        // Admin commands
        commands.add(new Notify());
        commands.add(new Purge());
        commands.add(new ResetChannels());
        //commands.add(new DeleteCourseChannels());

        // Everyone commands
        commands.add(new Bang());
        commands.add(new BangScore());
        commands.add(new Bet());
        commands.add(new Buy());
        commands.add(new Daily());
        commands.add(new Flip());
        commands.add(new Gift());
        commands.add(new Help());
        commands.add(new Hit());
        commands.add(new Id());
        commands.add(new Info());
        commands.add(new Market());
        commands.add(new MyHand());
        commands.add(new Ping());
        commands.add(new Profile());
        commands.add(new Stand());
    }
}
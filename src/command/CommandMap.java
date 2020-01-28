package command;

import command.commands.bang.Bang;
import command.commands.bang.BangScore;
import command.commands.bang.BangScores;
import command.commands.bang.MyBang;
import command.commands.blackjack.Bet;
import command.commands.blackjack.Hit;
import command.commands.blackjack.MyHand;
import command.commands.blackjack.Stand;
import command.commands.economy.Buy;
import command.commands.Help;
import command.commands.economy.Market;
import command.commands.admin.Purge;
import command.commands.economy.Wallet;
import command.commands.misc.Flip;
import command.commands.misc.Id;
import command.commands.misc.Info;
import command.commands.misc.Karma;
import command.commands.misc.Ping;
import command.commands.roles.Join;
import command.commands.roles.Leave;
import command.commands.roles.Roles;
import command.commands.bang.Daily;

import java.util.HashMap;

/**
 * Container class for a list of all the commands
 * available in the server.
 */
public final class CommandMap {

    private static HashMap<String, Command> commands;

    private CommandMap() {

    }

    static {
        commands = new HashMap<>();
        mapAllCommands();
    }

    /**
     * Commands list getter.
     *
     * @return the ArrayList of all commands
     */
    public static HashMap<String, Command> getCommands() {
        return commands;
    }

    private static void mapAllCommands() {
        // Admin commands
        commands.put(Purge.getKey(), new Purge());

        // Everyone commands
        commands.put(Bang.getKey(), new Bang());
        commands.put(BangScore.getKey(), new BangScore());
        commands.put(BangScores.getKey(), new BangScores());
        commands.put(Bet.getKey(), new Bet());
        commands.put(Buy.getKey(), new Buy());
        commands.put(Daily.getKey(), new Daily());
        commands.put(Flip.getKey(), new Flip());
        commands.put(Help.getKey(), new Help());
        commands.put(Hit.getKey(), new Hit());
        commands.put(Id.getKey(), new Id());
        commands.put(Info.getKey(), new Info());
        commands.put(Join.getKey(), new Join());
        commands.put(Karma.getKey(), new Karma());
        commands.put(Leave.getKey(), new Leave());
        commands.put(Market.getKey(), new Market());
        commands.put(MyBang.getKey(), new MyBang());
        commands.put(MyHand.getKey(), new MyHand());
        commands.put(Ping.getKey(), new Ping());
        commands.put(Roles.getKey(), new Roles());
        commands.put(Stand.getKey(), new Stand());
        commands.put(Wallet.getKey(), new Wallet());
    }
}

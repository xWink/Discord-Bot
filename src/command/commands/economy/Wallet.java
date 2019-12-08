package command.commands.economy;

import command.Command;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Wallet extends Command {

    private EconomyConnector ec;

    /**
     * Initializes the command's key to "!wallet".
     */
    public Wallet() {
        super("!wallet", false);
        ec = new EconomyConnector();
    }

    /**
     * Displays the amount of GryphCoins the user has.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        try {
            event.getChannel().sendMessage(event.getAuthor().getName()
                    + " has " + ec.getWealth(event.getAuthor().getIdLong())
                    + " GryphCoins").queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

package command.commands;

import command.Command;
import database.connectors.PingConnector;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Ping extends Command {

    private PingConnector pc;

    /**
     * Initializes command's key to "!ping".
     */
    public Ping() {
        super("!ping");
        pc = new PingConnector();
    }

    /**
     * Checks if string matches to key.
     *
     * @param string the user's input being compared to the key
     * @return true if input equals key
     */
    @Override
    public boolean keyMatches(String string) {
        return string.equalsIgnoreCase(getKey());
    }

    /**
     * Gets Discord's ping and outputs it for the user. Also
     * updates the user's row in the ping table if that ping is
     * their new max or min.
     *
     * @param event the message event that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        long authorId = event.getAuthor().getIdLong();

        int ping = (int) event.getJDA().getPing();
        event.getChannel().sendMessage("Pong! " + ping + " ms").queue();

        try {
            if (pc.isMax(authorId, ping)) {
                event.getChannel().sendMessage("That's your highest ping ever!").queue();
                pc.setMaxPing(authorId, ping);
            } else if (pc.isMin(authorId, ping)) {
                event.getChannel().sendMessage("That's your lowest ping ever!").queue();
                pc.setMinPing(authorId, ping);
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

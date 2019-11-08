package command;

import database.PingConnector;
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


    @Override
    public boolean keyMatches(String string) {
        return string.equalsIgnoreCase(getKey());
    }


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
            e.printStackTrace();
        }
    }
}

package command.commands.economy;

import command.Command;
import economy.WagerLimits;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class PaidPlay extends Command {

    public PaidPlay() {
        super("!paidplay", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey());
    }

    /**
     * Lets a player opt out of (or back into) games that risk their coins.
     */
    @Override
    public void start(MessageReceivedEvent event) {
        String[] args = event.getMessage().getContentRaw().split("\\s+");
        try {
            long user = event.getAuthor().getIdLong();
            if (args.length == 2 && args[1].equalsIgnoreCase("off")) {
                WagerLimits.setPaidPlay(user, false);
                event.getChannel().sendMessage("Paid play is now **off**. Free play still works.").queue();
            } else if (args.length == 2 && args[1].equalsIgnoreCase("on")) {
                WagerLimits.setPaidPlay(user, true);
                event.getChannel().sendMessage("Paid play is now **on**.").queue();
            } else {
                event.getChannel().sendMessage("Paid play is "
                        + (WagerLimits.isPaidPlayEnabled(user) ? "on" : "off")
                        + ". Say `!paidplay on` or `!paidplay off`.").queue();
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

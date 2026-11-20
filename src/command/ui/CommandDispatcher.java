package command.ui;

import command.Command;
import main.CommandList;
import main.Server;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/** Finds and runs the text command for a message, applying the channel rules. */
public final class CommandDispatcher {

    private CommandDispatcher() { }

    /**
     * Runs the command that matches the message content, if any.
     *
     * @param event the (real or synthetic) message event
     * @return true if a command matched, whether or not the channel allowed it
     */
    public static boolean dispatch(MessageReceivedEvent event) {
        String content = event.getMessage().getContentRaw();
        if (!content.startsWith("!")) return false;
        for (Command command : CommandList.getCommands()) {
            if (!command.keyMatches(content)) continue;
            long channelId = event.getChannel().getIdLong();
            if (command.isGlobal() || channelId == Server.BOTS_CHANNEL_ID || channelId == Server.SPAM_CHANNEL_ID) {
                Stats.command(command.getKey());
                Motion.begin(event.getAuthor().getIdLong());
                Skin.begin(event.getAuthor().getIdLong());
                try {
                    command.start(event);
                } finally {
                    Motion.end();
                    Skin.end();
                }
            } else {
                event.getChannel().sendMessage("`" + command.getKey() + "` works in <#" + Server.BOTS_CHANNEL_ID
                        + "> or <#" + Server.SPAM_CHANNEL_ID + ">.").queue();
            }
            return true;
        }
        return false;
    }
}

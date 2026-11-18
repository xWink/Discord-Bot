package command.commands.misc;

import command.Command;
import command.ui.Buttons;
import command.ui.Motion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

/** {@code !motion off} turns every animation into plain text for you; {@code !motion on} brings them back. */
public class MotionCommand extends Command {

    public MotionCommand() {
        super("!motion", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().matches("^!motion(\\s+.*)?$");
    }

    @Override
    public void start(MessageReceivedEvent event) {
        long user = event.getAuthor().getIdLong();
        String arg = event.getMessage().getContentRaw().trim().replaceFirst("(?i)^!motion\\s*", "").trim().toLowerCase();
        try {
            switch (arg) {
                case "off":
                case "reduce":
                    Motion.set(user, true);
                    event.getChannel().sendMessage("Animations are **off** for you. Games, lookups and your profile now answer in "
                            + "plain text and still images. `!motion on` brings them back.")
                            .setActionRow(Buttons.of(user, true, "!motion on", "Turn animations back on", ButtonStyle.SECONDARY)).queue();
                    return;
                case "on":
                    Motion.set(user, false);
                    event.getChannel().sendMessage("Animations are **on** again. `!motion off` turns them off.").queue();
                    return;
                case "":
                case "status": {
                    boolean off = Motion.reduced(user);
                    event.getChannel().sendMessage("Animations are **" + (off ? "off" : "on") + "** for you. Say `!motion "
                            + (off ? "on" : "off") + "` to change it. When they are off, every game and lookup sends its result "
                            + "as plain text instead of a GIF.").queue();
                    return;
                }
                default:
                    event.getChannel().sendMessage("Say `!motion off` or `!motion on`.").queue();
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

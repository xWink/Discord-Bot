package command.commands.misc;

import command.Command;
import command.ui.Skin;
import command.util.art.Theme;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/** {@code !theme light} or {@code !theme dark}: the palette of the pictures the bot draws for you. */
public class ThemeCommand extends Command {

    public ThemeCommand() {
        super("!theme", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().matches("^!theme(\\s+.*)?$");
    }

    @Override
    public void start(MessageReceivedEvent event) {
        long user = event.getAuthor().getIdLong();
        String arg = event.getMessage().getContentRaw().trim().replaceFirst("(?i)^!theme\\s*", "").trim();
        try {
            if (arg.isEmpty()) {
                event.getChannel().sendMessage("Your pictures are drawn in the **" + Skin.of(user).label
                        + "** theme. Say `!theme light` or `!theme dark` to change it.").queue();
                return;
            }
            Theme chosen = Theme.parse(arg);
            if (chosen == null) {
                event.getChannel().sendMessage("Say `!theme light` or `!theme dark`.").queue();
                return;
            }
            Skin.set(user, chosen);
            event.getChannel().sendMessage("Your pictures are now drawn in the **" + chosen.label
                    + "** theme. Games, your profile and course lookups use it from the next one.").queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

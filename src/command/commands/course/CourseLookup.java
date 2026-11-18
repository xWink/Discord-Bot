package command.commands.course;

import command.ui.Buttons;
import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Picks the one course a command is about. An exact code wins. Anything else (a bare number, words from a title)
 * is only accepted if it names a single course; if it could be several, the player is asked which, with a button
 * for each that runs the same command again. A command never guesses between courses.
 */
public final class CourseLookup {

    private static final int OFFERED = 5;

    private CourseLookup() { }

    /**
     * @param command the command to run again for the course that was meant, for example {@code !whynot}
     * @return the course, or null after the player has been told there is none or asked which was meant
     */
    public static Course resolve(MessageChannel channel, String query, String command) {
        CourseCatalog catalog = CourseCatalog.get();
        Course exact = catalog.find(query);
        if (exact != null) return exact;
        List<Course> found = catalog.match(query, OFFERED + 1);
        if (found.isEmpty()) {
            channel.sendMessage("I could not find a course matching `" + query + "`. Try a code like `cis1300` or words "
                    + "from the title.").queue();
            return null;
        }
        if (found.size() == 1) return found.get(0);
        EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0x99AAB5)).setTitle("Which course did you mean by \"" + shorten(query, 40) + "\"?");
        StringBuilder list = new StringBuilder();
        List<ItemComponent> buttons = new ArrayList<>();
        for (Course c : found.subList(0, Math.min(OFFERED, found.size()))) {
            list.append("**").append(c.code.replace("*", "\\*")).append("** ").append(c.title).append('\n');
            buttons.add(Buttons.of(0, false, command + " " + c.code, shorten(c.code + " " + c.title, 78), ButtonStyle.SECONDARY));
        }
        if (found.size() > OFFERED) list.append("\u2026and more: type the full code, like `").append(found.get(0).code.replace("*", "").toLowerCase()).append("`.");
        eb.setDescription(list.toString());
        channel.sendMessageEmbeds(eb.build()).setComponents(ActionRow.of(buttons)).queue();
        return null;
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1).trim() + "\u2026";
    }
}

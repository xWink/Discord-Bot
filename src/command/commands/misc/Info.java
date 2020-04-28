/*
 * Made by Jeremy Thorne.
 * GitHub: https://github.com/jeremyt123
 */
package command.commands.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

import command.Command;
import command.util.misc.CourseData;
import jdk.internal.jline.internal.Nullable;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.annotation.Nonnull;

public class Info extends Command {

    /**
     * Sets up the file reader for the tsv.
     */
    public Info() {
        super("!info", true);
    }

    /**
     * Key matches if the string equals exactly (ignoring case) the key.
     *
     * @param string the user's input being compared to the key
     * @return true if the key matches the string
     */
    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().matches("^" + getKey() + " .+$");
    }

    /**
     * Sets up the couse searching and outputs the course it finds.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(final MessageReceivedEvent event) {
        String[] strings = event.getMessage().getContentRaw().split(" ");
        MessageChannel channel = event.getChannel();

        if (strings.length < 2) {
            channel.sendMessage("To search for course info, say `!info <course ID>`").queue();
            return;
        }

        InputStream in = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("courses.tsv"));
        String courseLine = findCourseInTSV(strings[1], in);
        if (courseLine == null) {
            channel.sendMessage("Could not find a course with that ID").queue();
            return;
        }

        String[] split = courseLine.split("\t");
        CourseData data = new CourseData(split[0], split[1], split[2], split[3]);
        channel.sendMessage(embedCourseData(data).build()).queue();
    }

    /**
     * Searches a given TSV file for the line containing a given course ID.
     *
     * @return String with the tab-separated course info or null if the course is not found or an error occurs
     */
    @Nullable
    private String findCourseInTSV(@Nonnull String courseId, @Nonnull InputStream in) {
        String line, temp, tempId;
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                temp = line.split("\t")[0].split(" ")[0].replace("*", "").toLowerCase();
                tempId = courseId.replaceAll("\\*", "").toLowerCase();
                if (temp.equals(tempId))
                    return line.replaceAll("\"", "");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Creates an embed builder from a given CourseData object with a neat format.
     *
     * @param data the CourseData to be embedded
     * @return an EmbedBuilder object with a title and fields representing the given data
     */
    private EmbedBuilder embedCourseData(@Nonnull CourseData data) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(data.getCourseId());
        eb.addField("Restrictions:", data.getRestrictions().replaceAll("\\*", "\\\\*"), false);
        eb.addField("Prerequisites:", data.getPrerequisites().replaceAll("\\*", "\\\\*"), false);
        eb.addField("Description:", data.getDescription(), false);
        return eb;
    }
}

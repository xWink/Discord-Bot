package command.commands.misc;

import command.Command;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Join extends Command {

    /**
     * Initializes the command's key to "!join".
     */
    public Join() {
        super("!join", false);
    }

    /**
     * Allows user to join a role.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        String[] strings = message.split(" ");

        if (strings.length < 2) {
            event.getChannel().sendMessage("Command: !join <courseID>\n`Example: !join mcs2100`").queue();
            return;
        }

        String courseId = message.substring(message.indexOf(strings[1]));
        if (!event.getGuild().getRolesByName(courseId, true).isEmpty()) {
            attemptRoleAssignment(event, courseId);
            return;
        }

        if (!courseExists(courseId)) {
            event.getChannel().sendMessage("There is no course with that ID").queue();
            return;
        }

        //TODO: Need connector for the database stuff
    }

    private void attemptRoleAssignment(MessageReceivedEvent event, String roleName) {
        Guild guild = event.getGuild();
        MessageChannel channel = event.getChannel();
        List<Role> roles = guild.getRolesByName(roleName, true);

        if (guild.getMember(event.getAuthor()).getRoles().contains(roles.get(0))) {
            channel.sendMessage("You already have this role!").complete();

        } else if (guild.getTextChannelsByName(roleName, true).isEmpty()
                || !guild.getJDA().getCategoryById("556266020625711130").getTextChannels()
                .contains(guild.getTextChannelsByName(roleName, true).get(0))) {
            channel.sendMessage("I cannot set you to that role").complete();

        } else {
            guild.getController().addRolesToMember(event.getMember(), roles).queue();
            channel.sendMessage("Role " + roleName + " added to "
                    + event.getMember().getAsMention()).complete();
        }
    }

    /**
     * Searches the file for the course.
     *
     * @param courseId the ID of the course being searched for
     * @return true if course is found in the file
     */
    private boolean courseExists(String courseId) {
        String tsv = new File("").getAbsolutePath();
        tsv = tsv.replace("build/libs", "") + "res/courses.tsv";
        try (BufferedReader reader = new BufferedReader(new FileReader(tsv))) {
            String line, temp;
            while ((line = reader.readLine()) != null) {
                temp = line.split("\t")[0];
                if (temp.replace("*", "").toLowerCase()
                        .contains(courseId.replace("*", "").toLowerCase())) {
                    return true;
                }
            }
        } catch (IOException ignored) { }
        return false;
    }
}

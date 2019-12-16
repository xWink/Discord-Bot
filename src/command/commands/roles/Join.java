package command.commands.roles;

import command.Command;
import database.connectors.RolesConnector;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Join extends Command {

    private RolesConnector rs;
    private MessageReceivedEvent theEvent;
    private String courseId;

    /**
     * @see Command
     * Initializes the command's key to "!join".
     */
    public Join() {
        super("!join", false);
        rs = new RolesConnector();
    }

    /**
     * @see Leave
     * Allows user to join a role.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        String[] strings = message.split(" ");
        MessageChannel channel = event.getChannel();
        theEvent = event;

        if (strings.length < 2) {
            channel.sendMessage("Command: !join <courseID>\n`Example: !join mcs2100`").queue();
            return;
        }

        courseId = message.substring(message.indexOf(strings[1]));
        if (!event.getGuild().getRolesByName(courseId, true).isEmpty()) {
            attemptRoleAssignment();
            return;
        }

        if (!courseExists()) {
            channel.sendMessage("There is no course with that ID").queue();
            return;
        }

        applyForRole();
    }

    /**
     * Tries to assign a role based on courseId to a member.
     */
    private void attemptRoleAssignment() {
        Guild guild = theEvent.getGuild();
        MessageChannel channel = theEvent.getChannel();
        List<Role> roles = guild.getRolesByName(courseId, true);
        List<TextChannel> channels = guild.getTextChannelsByName(courseId, true);
        List<TextChannel> electiveChannels = guild.getJDA().getCategoryById("556266020625711130").getTextChannels();

        if (guild.getMember(theEvent.getAuthor()).getRoles().contains(roles.get(0)))
            channel.sendMessage("You already have this role!").complete();

        else if (channels.isEmpty() || !electiveChannels.contains(channels.get(0)))
            channel.sendMessage("I cannot set you to that role").complete();

        else {
            guild.getController().addRolesToMember(theEvent.getMember(), roles).queue();
            channel.sendMessage("Role " + courseId + " added to "
                    + theEvent.getMember().getAsMention()).complete();
        }
    }

    /**
     * Tries to apply for a role that doesn't yet exist.
     */
    private void applyForRole() {
        MessageChannel channel = theEvent.getChannel();
        try {
            if (rs.userAppliedForRole(courseId, theEvent.getAuthor().getIdLong())) {
                channel.sendMessage("You already applied for that role").queue();
                return;
            }

            if (rs.getNumApplications(courseId) < 3) {
                rs.applyForRole(courseId, theEvent.getAuthor().getIdLong());
                channel.sendMessage("Added your application to " + courseId + "!").queue();
                return;
            }

            // Create role/channel and apply them to applicants
            createRole();
            giveRoleToApplicants();

            channel.sendMessage("The channel you applied for was created! "
                    + "Only members of the channel can see it.").queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(theEvent, e);
        }
    }

    /**
     * Searches the file for the course.
     *
     * @return true if course is found in the file
     */
    private boolean courseExists() {
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

    /**
     * Creates a role and text channel with specific permissions
     * such that only people with the new role can see the new
     * channel.
     */
    private void createRole() {
        Guild guild = theEvent.getGuild();

        // Create role and channel
        guild.getController().createRole().setName(courseId).queue();
        guild.getController().createTextChannel(courseId).setParent(guild.getCategoriesByName("Electives", true).get(0)).queue(); // might be complete()
        TextChannel textChannel = guild.getTextChannelsByName(courseId, true).get(0);
        Role role = guild.getRolesByName(courseId, true).get(0);

        // Set new channel permissions
        if (textChannel.getPermissionOverride(role) == null)
            textChannel.createPermissionOverride(role).queue(); // might be complete()

        // Let people with the specified role see the channel and read/send messages
        textChannel.getPermissionOverride(role).getManager().grant(Permission.VIEW_CHANNEL).queue();
        textChannel.getPermissionOverride(role).getManager().grant(Permission.MESSAGE_READ).queue();

        // Prevent everyone from seeing the channel
        if (textChannel.getPermissionOverride(guild.getRolesByName("@everyone", true).get(0)) == null)
            textChannel.createPermissionOverride(guild.getRolesByName("@everyone", true).get(0)).queue(); // might be complete()

        textChannel.getPermissionOverride(guild.getRolesByName("@everyone", true).get(0)).getManager().deny(Permission.MESSAGE_READ).queue();

        // Do not let people with this role do @everyone
        textChannel.getPermissionOverride(role).getManager().deny(Permission.MESSAGE_MENTION_EVERYONE).queue();
    }

    /**
     * Gives a newly created role to all people who applied for it.
     *
     * @throws SQLException may be thrown when interacting with the database
     */
    private void giveRoleToApplicants() throws SQLException {
        ArrayList<Long> applicants = rs.getApplicantIds(courseId);
        List<Role> roles = theEvent.getGuild().getRolesByName(courseId, true);

        for (int i = 0; i < 4; i++) {
            theEvent.getGuild().getController()
                    .addRolesToMember(theEvent.getGuild().getMemberById(applicants.get(i)), roles).queue();
        }
        theEvent.getGuild().getController().addRolesToMember(theEvent.getMember()).queue();
    }
}

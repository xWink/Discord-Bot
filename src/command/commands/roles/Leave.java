package command.commands.roles;

import command.Command;
import database.connectors.RolesConnector;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class Leave extends Command {

    private RolesConnector rs;
    private MessageReceivedEvent theEvent;
    private String courseId;

    /**
     * @see Command
     * Initializes the command's key to "!leave".
     */
    public Leave() {
        super("!leave", false);
        rs = new RolesConnector();
    }

    /**
     * Compares a string to the command's key and checks if that
     * string starts with the key.
     *
     * @param string the user's input being compared to the key
     * @return returns true if the key matches and false otherwise
     */
    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey());
    }

    /**
     * @see Join
     * Allows user to leave a role.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        if (event.getMember() == null)
            return;

        String message = event.getMessage().getContentRaw();
        String[] strings = message.split(" ");
        MessageChannel channel = event.getChannel();
        theEvent = event;

        if (strings.length != 2) {
            channel.sendMessage("Command: !leave <group name>\n`Example: !leave mcs2100`").queue();
            return;
        }

        courseId = message.substring(message.indexOf(strings[1]));
        List<Role> guildRoles = event.getGuild().getRolesByName(courseId, true);

        if (!guildRoles.isEmpty() && event.getMember().getRoles().contains(guildRoles.get(0))) {
            if (leaveRole())
                channel.sendMessage("Removed " + courseId
                        + " from " + event.getMember().getAsMention()).queue();
            else
                channel.sendMessage("I cannot remove you from this role!").queue();
        } else {
            if (removeApplication())
                channel.sendMessage("Your application for "
                        + courseId + " has been removed").queue();
            else
                channel.sendMessage("You don't have an application for this role!").queue();
        }
    }

    private boolean leaveRole() {
        Guild guild = theEvent.getGuild();
        List<TextChannel> channels = guild.getTextChannelsByName(courseId, true);
        Category electives = guild.getJDA().getCategoryById("556266020625711130");

        // If role is in the Electives category
        if (!channels.isEmpty() && electives != null && electives.getTextChannels().contains(channels.get(0))) {
            guild.removeRoleFromMember(Objects.requireNonNull(theEvent.getMember()),
                    guild.getRolesByName(courseId, true).get(0)).queue();
            return true;
        }
        return false;
    }

    private boolean removeApplication() {
        long userId = theEvent.getAuthor().getIdLong();
        try {
            if (rs.userAppliedForRole(courseId, userId)) {
                rs.removeApplication(courseId, userId);
                return true;
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(theEvent, e);
        }
        return false;
    }
}

package command.commands.admin;

import command.AdminCommand;
import command.Command;
import main.Server;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Notify extends Command implements AdminCommand {

    /**
     * Initializes the command's key to "!notify".
     */
    public Notify() {
        super("!notify", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.startsWith(getKey());
    }

    @Override
    public void start(MessageReceivedEvent event) {
        if (!AdminCommand.memberIsAdmin(event.getMember())) {
            return;
        }

        MessageChannel channel = event.getChannel();
        Guild guild = event.getGuild();

        // Get target role and message
        String[] contents = event.getMessage().getContentRaw().split(" ");
        if (contents.length < 3) {
            channel.sendMessage("Usage: !notify <rolename> <message>").queue();
            return;
        }
        String roleName = contents[1].toLowerCase().replaceAll("\\*", "");
        String message = String.join(" ", Arrays.asList(contents).subList(2, contents.length));

        // Find role
        Role targetRole = null;
        List<Role> roles = guild.getRoles();
        for (Role role : roles) {
            if (role.getName().toLowerCase().replaceAll("\\*", "").startsWith(roleName)) {
                targetRole = role;
            }
        }
        if (targetRole == null) {
            channel.sendMessage("Could not find a role by that name").queue();
            return;
        }

        // Get all members that need to be mentioned
        List<Member> members = Stream.concat(
                guild.getMembersWithRoles(targetRole, guild.getRoleById(Server.NOTIFY_ROLE_ID)).stream(),
                guild.getMembersWithRoles(targetRole, guild.getRoleById(Server.ALL_ROLE_ID)).stream()
        ).collect(Collectors.toList());
        if (members.isEmpty()) {
            channel.sendMessage("Nobody to notify").queue();
            return;
        }

        // Create message to mention everyone with All role and target role
        String mentions = "";
        for (Member member : members) {
            mentions = mentions.concat(member.getAsMention() + " ");
            if (mentions.length() >= 1900) {
                channel.sendMessage(mentions);
                mentions = "";
            }
        }

        // Mention all target members then send related message
        channel.sendMessage(mentions).queue(theMessage -> channel.sendMessage(message).queue());
    }
}

package command;

import main.Server;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface AdminCommand {

    /**
     * Checks if a member is an admin or the owner of the server.
     *
     * @param member the member of the server
     * @return true if the member is an admin or owner
     */
    default boolean memberIsAdmin(Member member) {
        if (member != null)
            return member.getRoles().contains(member.getGuild().getRoleById(Server.ADMIN_ROLE_ID)) || member.isOwner();
        return false;
    }
}

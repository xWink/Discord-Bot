package command;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public abstract class AdminCommand extends Command {

    /**
     * Initializes the command's key.
     *
     * @param theKey the command's key
     * @param isGlobal whether the command can be used in any channel
     */
    protected AdminCommand(String theKey, boolean isGlobal) {
        super(theKey, isGlobal);
    }

    /**
     * Checks if a member is an admin or the owner of the server.
     *
     * @param member the member of the server
     * @return true if the member is an admin or owner
     */
    protected final boolean isAdmin(Member member) {
        return member.getRoles().contains(member.getGuild().getRoleById("486635066928136194"))
                || member.isOwner();
    }

    /**
     * Overrides the start method so that the command only runs if the member who
     * triggered it is a server admin or owner.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public final void start(MessageReceivedEvent event) {
        if (isAdmin(event.getMember()))
            runCommand(event);
    }

    protected abstract void runCommand(MessageReceivedEvent event);
}

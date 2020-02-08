package command.commands.roles;

import command.ResponseCommand;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class RemoveRole extends ResponseCommand {

    private String roleName;
    private ResponseHandler handler;

    /**
     * Initializes the command's key to "!removerole".
     */
    public RemoveRole() {
        super("!removerole", false);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey()) && string.split(" ").length == 2;
    }

    @Override
    public void respond(MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().toLowerCase().equals("yes")) {
            Role role = event.getGuild().getRolesByName(roleName, true).get(0);
            if (role == null) {
                event.getChannel().sendMessage("Could not find such a role").queue();
            } else if (!(role.getName().equals("Blue") || role.getName().equals("Pink") || role.getName().equals("Green")
                    || role.getName().equals("Orange") || role.getName().equals("Purple"))) {
                event.getChannel().sendMessage("I cannot remove this role").queue();
            } else {
                event.getGuild().removeRoleFromMember(event.getAuthor().getId(), role).queue();
                event.getChannel().sendMessage("You no longer have this role").queue();
            }
        } else if (event.getMessage().getContentRaw().toLowerCase().equals("no")) {
            handler.expire();
        }
    }

    @Override
    public void start(MessageReceivedEvent event) {
        if (event.getMember() == null)
            return;

        roleName = event.getMessage().getContentRaw().split(" ")[1];
        handler = new ResponseHandler(event.getChannel().getIdLong(), event.getAuthor().getIdLong(), event.getJDA());
    }
}

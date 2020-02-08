package command.commands.roles;

import command.ResponseCommand;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public class RemoveRole extends ResponseCommand {

    private String roleName;
    private ResponseHandler handler;
    EconomyConnector ec;

    /**
     * Initializes the command's key to "!removerole".
     */
    public RemoveRole() {
        super("!removerole", false);
        ec = new EconomyConnector();
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey()) && string.split(" ").length == 2;
    }

    @Override
    public void respond(MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().toLowerCase().equals("yes")) {
            String colorName;
            List<Role> roles = event.getGuild().getRolesByName(roleName, true);

            if (roles.isEmpty()) {
                event.getChannel().sendMessage("Could not find such a role").queue();
                handler.expire();
                return;
            }

            try {
                colorName = ec.getUserRow(event.getAuthor().getIdLong()).getString("role_colour");
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
                return;
            }

            if (colorName != null && colorName.toLowerCase().equals(roleName)) {
                try {
                    ec.resetUserRole(event.getAuthor().getIdLong());
                    event.getGuild().removeRoleFromMember(event.getAuthor().getId(), roles.get(0)).queue();
                    event.getChannel().sendMessage("Role removed!").queue();
                    handler.expire();
                } catch (Exception e) {
                    printStackTraceAndSendMessage(event, e);
                }
            }

            else {
                event.getChannel().sendMessage("I cannot remove this role").queue();
                handler.expire();
            }
        }

        else if (event.getMessage().getContentRaw().toLowerCase().equals("no")) {
            event.getChannel().sendMessage("Cancelled role removal").queue();
            handler.expire();
        }
    }

    @Override
    public void start(MessageReceivedEvent event) {
        if (event.getMember() == null)
            return;

        event.getChannel().sendMessage("Are you sure you want to remove this role? "
                + "(Say `yes` to confirm or `no` to cancel)").queue();

        roleName = event.getMessage().getContentRaw().split(" ")[1].toLowerCase();
        handler = new ResponseHandler(event.getChannel().getIdLong(), event.getAuthor().getIdLong(), event.getJDA());
    }
}

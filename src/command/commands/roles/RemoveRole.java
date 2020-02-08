package command.commands.roles;

import command.ResponseCommand;
import database.connectors.EconomyConnector;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

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
            Role role = event.getGuild().getRolesByName(roleName, true).get(0);

            if (role == null) {
                event.getChannel().sendMessage("Could not find such a role").queue();
                return;
            }

            try {
                colorName = ec.getUserRow(event.getAuthor().getIdLong()).getString("role_color");
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
                return;
            }

            if (colorName != null && colorName.equals(roleName)) {
                try {
                    ec.resetUserRole(event.getAuthor().getIdLong());
                    event.getGuild().removeRoleFromMember(event.getAuthor().getId(), role).queue();
                    event.getChannel().sendMessage("Role removed!").queue();
                } catch (Exception e) {
                    printStackTraceAndSendMessage(event, e);
                }
            }

            else {
                event.getChannel().sendMessage("I cannot remove this role").queue();
            }
        }

        else if (event.getMessage().getContentRaw().toLowerCase().equals("no")) {
            handler.expire();
            event.getChannel().sendMessage("Cancelled role removal").queue();
        }
    }

    @Override
    public void start(MessageReceivedEvent event) {
        if (event.getMember() == null)
            return;

        event.getChannel().sendMessage("Are you sure you want to remove this role? "
                + "(Say `yes` to confirm or `no` to cancel)").queue();

        roleName = event.getMessage().getContentRaw().split(" ")[1];
        handler = new ResponseHandler(event.getChannel().getIdLong(), event.getAuthor().getIdLong(), event.getJDA());
    }
}

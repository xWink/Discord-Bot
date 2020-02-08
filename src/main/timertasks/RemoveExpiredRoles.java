package main.timertasks;

import database.connectors.EconomyConnector;
import main.Server;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.sql.ResultSet;
import java.util.TimerTask;

public class RemoveExpiredRoles extends TimerTask {

    private EconomyConnector ec;

    /**
     * Initializes EconomyConnector.
     */
    public RemoveExpiredRoles() {
        ec = new EconomyConnector();
    }

    /**
     * Finds all users with expired roles in the database and resets
     * their role to null and expiry to 0.
     */
    @Override
    public void run() {
        try {
            ResultSet rs = ec.getExpiredRoles();

            // For each user with expired roles
            while (rs.next()) {
                // Get member and role
                Guild guild = Server.getApi().getGuildById(Server.getGuild());
                if (guild == null)
                    return;
                Member member = guild.getMemberById(rs.getLong("user"));
                if (member == null)
                    return;
                Role role = guild.getRolesByName(rs.getString("role_colour"), true).get(0);

                // Remove role from member
                guild.removeRoleFromMember(member, role).queue();
                ec.resetUserRole(member.getUser().getIdLong());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

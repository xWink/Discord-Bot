package main.timertasks;

import economy.SeasonService;
import main.Server;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.Set;
import java.util.TimerTask;

/**
 * Closes the previous month when due, then makes the "Season Champion" role
 * match the entire current winner list, so former winners lose it even if they
 * fall outside the top 10.
 */
public class SeasonTask extends TimerTask {

    private static final String ROLE_NAME = "Season Champion";

    @Override
    public void run() {
        try {
            SeasonService.closePreviousIfDue();
            Guild guild = Server.API.getGuildById(Server.GUILD_ID);
            if (guild == null) return;
            java.util.List<Role> roles = guild.getRolesByName(ROLE_NAME, true);
            if (roles.isEmpty()) return;
            Role role = roles.get(0);
            Set<Long> winners = SeasonService.currentWinners();
            for (Member m : guild.getMembersWithRoles(role)) {
                if (!winners.contains(m.getIdLong())) guild.removeRoleFromMember(m, role).queue();
            }
            for (long id : winners) {
                Member m = guild.getMemberById(id);
                if (m != null && !m.getRoles().contains(role)) guild.addRoleToMember(m, role).queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

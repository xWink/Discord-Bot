package main.timertasks;

import database.connectors.EconomyConnector;
import economy.ShopService;
import main.Server;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.TimerTask;

public class RemoveExpiredRoles extends TimerTask {

    /** A purchase still pending after this long never got an answer from Discord. */
    private static final long STALE_PENDING_MS = 10 * 60 * 1000;

    private EconomyConnector ec;

    /**
     * Initializes EconomyConnector.
     */
    public RemoveExpiredRoles() {
        ec = new EconomyConnector();
    }

    /**
     * Removes each expired rental independently. One missing member or role never
     * stops the others, and a removal Discord rejects is kept to retry next run.
     * Also refunds purchases stuck pending since a restart.
     */
    @Override
    public void run() {
        Guild guild = Server.API.getGuildById(Server.GUILD_ID);
        if (guild == null)
            return;

        try {
            for (ShopService.Pending p : ShopService.getInstance().findStalePending(STALE_PENDING_MS)) {
                try {
                    if (ShopService.getInstance().failAndRefund(p.purchaseId)) {
                        Member member = guild.getMemberById(p.user);
                        Role role = guild.getRoleById(p.roleId);
                        if (member != null && role != null) guild.removeRoleFromMember(member, role).queue();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            for (EconomyConnector.ExpiredRole expired : ec.getExpiredRoles()) {
                try {
                    processExpiry(guild, expired);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processExpiry(Guild guild, EconomyConnector.ExpiredRole expired) throws Exception {
        Member member = guild.getMemberById(expired.user);
        Role role = expired.roleId != 0 ? guild.getRoleById(expired.roleId) : null;
        if (role == null && expired.roleName != null && !guild.getRolesByName(expired.roleName, true).isEmpty()) {
            role = guild.getRolesByName(expired.roleName, true).get(0);
        }

        // Nobody to take it from, or the role no longer exists: the rental is over.
        if (member == null || role == null || !member.getRoles().contains(role)) {
            ec.resetUserRole(expired.user);
            return;
        }

        guild.removeRoleFromMember(member, role).queue(
                ok -> {
                    try {
                        ec.resetUserRole(expired.user);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                failure -> System.out.println("Could not remove expired role for " + expired.user + "; will retry"));
    }
}

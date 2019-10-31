package DiscordBot.util.database;

import DiscordBot.RoleBot;
import DiscordBot.util.bang_util.BangHighScores;
import DiscordBot.util.bang_util.GetBangScores;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import static DiscordBot.util.database.DatabaseUtil.getConnection;

public class UpdateRoles {

    public static void removeExpiredColours(){

        Guild guild = RoleBot.api.getGuildById(Long.parseLong(RoleBot.config.guildId)); // Current guild
        Connection conn;
        Date date = new Date();

        // Connect to database
        if ((conn = getConnection()) == null){
            System.out.println("Could not connect to database to remove expired colours");
            return;
        }

        try{
            // Find all expired roles
            ResultSet rs = conn.prepareStatement("SELECT user, role_colour FROM economy WHERE role_expiry > 0 AND "+
                    "role_expiry < " + date.getTime()).executeQuery();

            // For each user with expired roles
            while (rs.next()){
                // Get member and role
                Member member = guild.getMemberById(rs.getLong("user"));
                Role role = guild.getRolesByName(rs.getString("role_colour"), true).get(0);

                // Remove role from member
                guild.getController().removeSingleRoleFromMember(member, role).complete();

                // Update database
                conn.prepareStatement("UPDATE economy SET role_expiry = 0, role_colour = NULL " +
                        "WHERE user = " + member.getUser().getIdLong()).executeUpdate();
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }

    public static void updateBangRoles(){

        Guild guild = RoleBot.api.getGuildById(Long.parseLong(RoleBot.config.guildId)); // Current guild
        BangHighScores highScores = GetBangScores.getHighScores(guild); // Get high scores

        if (highScores != null) {
            Role attemptsRole = guild.getRolesByName("Addict", true).get(0);
            Role luckyRole = guild.getRolesByName("Lucky Duck", true).get(0);
            Role unluckyRole = guild.getRolesByName("Snake Eyes", true).get(0);
            Role wealthRole = guild.getRolesByName("Scrooge McDuck", true).get(0);

            // Remove role from loser and add role to new winner
            // Most attempts
            giveAndTakeBangRoles(guild, attemptsRole, highScores.getMostAttemptsPlayer().getName());
            // Luckiest
            giveAndTakeBangRoles(guild, luckyRole, highScores.getLuckiest().getName());
            // Unluckiest
            giveAndTakeBangRoles(guild, unluckyRole, highScores.getUnluckiest().getName());
            // Most jams
            giveAndTakeBangRoles(guild, wealthRole, highScores.getWealthiest().getName());
        }
    }

    private static void giveAndTakeBangRoles(Guild guild, Role role, String name){

        try {
            if (guild.getMembersWithRoles(role).isEmpty() || !guild.getMembersWithRoles(role).get(0).getUser().getName().equals(name)) {
                if (guild.getMembersWithRoles(role).size() > 0)
                    guild.getController().removeRolesFromMember(guild.getMembersWithRoles(role).get(0), role).queue();

                guild.getController().addRolesToMember(guild.getMembersByName(name, true).get(0), role).queue();
                guild.getTextChannelById("551828950871965696").sendMessage("Congrats " + name + " on your new role!").complete();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}

package DiscordBot.util.bang_util;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

public class UpdateBangRoles {

    public static void updateBangRoles(){

        Guild guild = DiscordBot.MyEventListener.guild; // Current guild
        BangHighScores highScores = GetBangScores.getBangScores(guild); // Get high scores

        if (highScores != null) {
            Role attemptsRole = guild.getRolesByName("Addict", true).get(0);
            Role luckyRole = guild.getRolesByName("Lucky Duck", true).get(0);
            Role unluckyRole = guild.getRolesByName("Snake Eyes", true).get(0);
            Role jamRole = guild.getRolesByName("Too Angry To Die", true).get(0);

            // Remove role from loser and add role to new winner
            // Most attempts
            giveAndTakeBangRoles(guild, attemptsRole, highScores.getMostAttemptsPlayer().getName());
            // Luckiest
            giveAndTakeBangRoles(guild, luckyRole, highScores.getLuckiest().getName());
            // Unluckiest
            giveAndTakeBangRoles(guild, unluckyRole, highScores.getUnluckiest().getName());
            // Most jams
            giveAndTakeBangRoles(guild, jamRole, highScores.getMostJamsPlayer().getName());
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

package DiscordBot.helpers;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import src.main.java.DiscordBot.helpers.BangHighScores;

public class UpdateBangRoles {

    public static void updateBangRoles(){

        final String path3 = DiscordBot.MyEventListener.path3; // roulette file path
        Guild guild = DiscordBot.MyEventListener.guild; // Current guild
        BangHighScores highScores = GetBangScores.getBangScores(path3,guild); // Get high scores

        if (highScores != null) {
            Role attemptsRole = guild.getRolesByName("Addict", true).get(0);
            Role deathsRole = guild.getRolesByName("Dead Ringer", true).get(0);
            Role luckyRole = guild.getRolesByName("Lucky Duck", true).get(0);
            Role unluckyRole = guild.getRolesByName("Snake Eyes", true).get(0);

            // Remove special roles from everyone
            guild.getController().removeRolesFromMember(guild.getMembersWithRoles(deathsRole).get(0),deathsRole).queue();
            guild.getController().removeRolesFromMember(guild.getMembersWithRoles(luckyRole).get(0),luckyRole).queue();
            guild.getController().removeRolesFromMember(guild.getMembersWithRoles(unluckyRole).get(0),unluckyRole).queue();

            // Remove role from loser and add role to new winner
            // Most attempts
            if (guild.getMembersWithRoles(attemptsRole).size() == 0 || !guild.getMembersWithRoles(attemptsRole).get(0).getUser().getName().equals(highScores.mostAttemptsPlayer)) {
                if (guild.getMembersWithRoles(attemptsRole).size() > 0)
                    guild.getController().removeRolesFromMember(guild.getMembersWithRoles(attemptsRole).get(0), attemptsRole).queue();

                guild.getController().addRolesToMember(guild.getMembersByName(highScores.mostAttemptsPlayer, true).get(0), attemptsRole).queue();
System.out.println("Most attempts: "+highScores.mostAttemptsPlayer);
            }
            // Most deaths
            if (guild.getMembersWithRoles(deathsRole).size() == 0 || !guild.getMembersWithRoles(deathsRole).get(0).getUser().getName().equals(highScores.mostDeathsPlayer)) {
                if (guild.getMembersWithRoles(deathsRole).size() > 0)
                    guild.getController().removeRolesFromMember(guild.getMembersWithRoles(deathsRole).get(0), deathsRole).queue();

                guild.getController().addRolesToMember(guild.getMembersByName(highScores.mostDeathsPlayer, true).get(0), deathsRole).queue();
            }
            // Luckiest
            if (guild.getMembersWithRoles(luckyRole).size() == 0 || !guild.getMembersWithRoles(luckyRole).get(0).getUser().getName().equals(highScores.luckiest)) {
                if (guild.getMembersWithRoles(luckyRole).size() > 0)
                    guild.getController().removeRolesFromMember(guild.getMembersWithRoles(luckyRole).get(0), luckyRole).queue();

                guild.getController().addRolesToMember(guild.getMembersByName(highScores.luckiest, true).get(0), luckyRole).queue();
            }
            // Unluckiest
            if (guild.getMembersWithRoles(unluckyRole).size() == 0 || !guild.getMembersWithRoles(unluckyRole).get(0).getUser().getName().equals(highScores.unluckiest)) {
                if (guild.getMembersWithRoles(unluckyRole).size() > 0)
                    guild.getController().removeRolesFromMember(guild.getMembersWithRoles(unluckyRole).get(0), unluckyRole).queue();

                guild.getController().addRolesToMember(guild.getMembersByName(highScores.unluckiest, true).get(0), unluckyRole).queue();
            }
        }
    }
}

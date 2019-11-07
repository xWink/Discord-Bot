package main.eventlistener;

import main.Config;
import main.RoleBot;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class JoinLeaveEventListener extends ListenerAdapter {
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = RoleBot.getJDA().getGuildById(Config.getGuildId());
        //general channel ID = 486633949154770946
        TextChannel generalChannel = guild.getTextChannelsByName("general", true).get(0);
        //bots channel ID = 551828950871965696
        TextChannel botsChannel = guild.getTextChannelsByName("bots", true).get(0);

        if (generalChannel == null){
            System.out.println("Could not find general channel!");
            return;
        }

        generalChannel.sendMessage("Welcome " + event.getUser().getAsMention() +
                "! Feel free to ask any questions in here, we are always looking to help each other out!\n\n" +
                "If you want to play with our bot, made in-house, go to " + botsChannel.getAsMention() +
                " and say `!help` :smiley:").queue();
    }
}

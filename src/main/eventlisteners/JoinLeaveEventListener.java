package main.eventlisteners;

import main.Server;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class JoinLeaveEventListener extends ListenerAdapter {

    /**
     * Sends a message on general that provides guidance to new members when they join.
     * @param event the GuildMemberJoinEvent
     */
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {

        if (event.getGuild().getTextChannelById(Server.GENERAL_CHANNEL_ID) == null) {
            System.out.println("Could not find general channel!");
            return;
        }

        TextChannel channel = event.getGuild().getTextChannelById(Server.GENERAL_CHANNEL_ID);
        TextChannel botChannel = event.getGuild().getTextChannelById(Server.BOTS_CHANNEL_ID);

        if (channel != null && botChannel != null) {
            channel.sendMessage("Welcome " + event.getUser().getAsMention()
                    + "! Feel free to ask any questions in here, we are always looking to help each other out!\n\n"
                    + "If you want to play with our bot, made in-house, go to " + botChannel.getAsMention()
                    + " and say `!help` :smiley:").queue();
        }
    }
}

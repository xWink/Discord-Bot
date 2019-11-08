package main.eventlistener;

import main.Server;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class JoinLeaveEventListener extends ListenerAdapter {

    /**
     * Sends a message on general that provides guidance to new members when they join.
     * @param event the GuildMemberJoinEvent
     */
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {

        if (Server.getGeneralChannel() == null) {
            System.out.println("Could not find general channel!");
            return;
        }

        Server.getGeneralChannel().sendMessage("Welcome " + event.getUser().getAsMention()
                + "! Feel free to ask any questions in here, we are always looking to help each other out!\n\n"
                + "If you want to play with our bot, made in-house, go to " + Server.getBotsChannel().getAsMention()
                + " and say `!help` :smiley:").queue();
    }
}

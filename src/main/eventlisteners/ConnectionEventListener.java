package main.eventlisteners;

import main.Server;
import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class ConnectionEventListener extends ListenerAdapter {

    /**
     * When the bot is ready, saves the guild it is connected to
     * in memory, then prints "Ready!".
     *
     * @param event the ReadyEvent that occurs when the bot activates
     */
    @Override
    public void onReady(ReadyEvent event) {
        try {
            //Server.setGuild(event.getJDA().getGuildById(486633949154770944L));
            //Server.setGeneralChannel(Server.getServer().getGuild().getTextChannelById(486633949154770946L));
            //Server.setBotsChannel(Server.getServer().getGuild().getTextChannelById(551828950871965696L));
            System.out.println("Ready!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints "attempting to reconnect" when disconnecting.
     * It is unknown why this causes the bot to reconnect
     * or why the bot refuses to reconnect if the method
     * is left empty.
     *
     * @param event the disconnection event
     */
    @Override
    public void onDisconnect(DisconnectEvent event) {
        System.out.println("Attempting reconnect");
    }
}

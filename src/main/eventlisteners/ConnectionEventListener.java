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
        Server.setGuild(event.getJDA().getGuildById(486633949154770944L));
        Server.setGeneralChannel(Server.getGuild().getTextChannelById(486633949154770946L));
        System.out.println("Ready!");
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

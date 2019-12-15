package main.eventlisteners;

import main.Server;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class ConnectionEventListener extends ListenerAdapter {

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("READY!");
        for (Guild guild : Server.getApi().getGuilds())
            System.out.println(guild.getName());
        for (Guild guild : Server.getApi().getSelfUser().getMutualGuilds())
            System.out.println(guild.getName());
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

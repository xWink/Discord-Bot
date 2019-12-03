package main.eventlisteners;

import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class ConnectionEventListener extends ListenerAdapter {
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

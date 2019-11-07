package main.eventlistener;

import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class ConnectionEventListener extends ListenerAdapter {
    @Override
    public void onDisconnect(DisconnectEvent event) {
        System.out.println("Attempting reconnect");
    }
}

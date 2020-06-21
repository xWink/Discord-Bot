package main.eventlisteners;

import com.google.common.collect.ImmutableList;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class EventListeners {

    private EventListeners() {}

    public static final ImmutableList<ListenerAdapter> ALL = ImmutableList.of(
            new DisconnectEventListener(),
            new MessageBulkDeleteEventListener(),
            new MessageDeleteEventListener(),
            new MessageReceivedEventListener(),
            new MessageUpdateEventListener(),
            new ReactionEventListener()
    );
}

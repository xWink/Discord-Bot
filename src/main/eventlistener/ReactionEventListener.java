package main.eventlistener;

import database.KarmaConnector;
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class ReactionEventListener extends ListenerAdapter {

    private KarmaConnector kc;


    /**
     * Initializes KarmaConnector.
     */
    public ReactionEventListener() {
        kc = new KarmaConnector();
    }


    /**
     * Gets the ID number of the user whose message is being affected.
     *
     * @param event the event that occurred on the message (ie. MessageReactionAdd)
     * @return the ID number of the message author
     */
    private long getMessageAuthId(GenericMessageReactionEvent event) {
        return event.getTextChannel().getMessageById(event.getMessageId())
                .complete().getAuthor().getIdLong();
    }


    /**
     * Adds an upVote/downVote to the user's line in the table if the correct
     * reaction is added to their message.
     *
     * @param event the MessageReactionAdd event on a user's message
     */
    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        long messageAuthId = getMessageAuthId(event);
        if (event.getUser().getIdLong() != messageAuthId) {
            // If new user, add them to table
            if (!kc.userExists(messageAuthId)) {
                kc.addUser(messageAuthId);
            }
            // If upVote, add upVotes
            if (event.getReactionEmote().getEmote().getName().startsWith("upvote")) {
                kc.updateUpVotes(messageAuthId, 1);
            }
            // If downVote, add downVotes
            if (event.getReactionEmote().getEmote().getName().startsWith("downvote")) {
                kc.updateDownVotes(messageAuthId, 1);
            }
        }
    }


    /**
     * Removes an upVote/downVote from the user's line in the table if the correct
     * reaction is removed from their message.
     *
     * @param event the MessageReactionRemove event on a user's message
     */
    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        long messageAuthId = getMessageAuthId(event);
        if (event.getUser().getIdLong() != messageAuthId) {
            if (!kc.userExists(messageAuthId)) {
                kc.addUser(messageAuthId);
            }
            // If upVote removed, remove upVote
            if (event.getReactionEmote().getEmote().getName().startsWith("upvote")) {
                kc.updateUpVotes(messageAuthId, -1);
            }
            // If downVote removed, remove downVote
            if (event.getReactionEmote().getEmote().getName().startsWith("downvote")) {
                kc.updateDownVotes(messageAuthId, -1);
            }
        }
    }
}

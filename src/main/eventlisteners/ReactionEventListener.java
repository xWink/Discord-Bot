package main.eventlisteners;

import database.connectors.KarmaConnector;
import main.Server;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

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
        return event.getTextChannel().retrieveMessageById(event.getMessageId())
                .complete().getAuthor().getIdLong();
    }


    /**
     * Adds an upVote/downVote to user's Karma or provides roles associated with
     * certain message reactions
     *
     * @param event the MessageReactionAdd event on a user's message
     */
    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        long messageAuthId = getMessageAuthId(event);
        if (event.getMember() == null || event.getUser() == null || event.getUser().isBot()) {
            return;
        }

        if (event.getReactionEmote().getIdLong() == Server.CHECK_EMOJI_ID) {
            // User agrees to ToS
            if (event.getMessageIdLong() == Server.WELCOME_MESSAGE_ID) {
                addToSRole(event);
            }
            // Joining course channel
            else if (event.getChannel().getIdLong() == Server.CHANNELS_CHANNEL_ID) {
                String content = event.getTextChannel().retrieveMessageById(event.getMessageId()).complete().getContentRaw();
                Role role = event.getGuild().getRolesByName(content, false).get(0);
                if (role != null) event.getGuild().addRoleToMember(event.getMember(), role).queue();
            }
        }

        if (event.getUserIdLong() == getMessageAuthId(event)) {
            return;
        }

        // Handle upvotes
        try {
            // If upVote, add upVotes
            if (event.getReactionEmote().getEmote().getName().startsWith("upvote")) {
                if (!kc.userExists(messageAuthId)) {
                    kc.addUser(messageAuthId);
                }
                kc.updateUpVotes(messageAuthId, 1);
            }
            // If downVote, add downVotes
            if (event.getReactionEmote().getEmote().getName().startsWith("downvote")) {
                if (!kc.userExists(messageAuthId)) {
                    kc.addUser(messageAuthId);
                }
                kc.updateDownVotes(messageAuthId, 1);
            }
        } catch (Exception ignored) {}
    }

    private void addToSRole(MessageReactionAddEvent event) {
        if (event.getMember() == null) {
            return;
        }

        Role tosRole = Objects.requireNonNull(event.getGuild().getRoleById(Server.TOS_ROLE_ID));
        TextChannel channel = Objects.requireNonNull(event.getGuild().getTextChannelById(Server.CHANNELS_CHANNEL_ID));
        if (!event.getMember().getRoles().contains(tosRole)) {
            event.getGuild().addRoleToMember(event.getMember().getId(), tosRole).queue();
            channel.sendMessage(event.getMember().getAsMention())
                    .queue(message -> channel.deleteMessageById(message.getId()).queue());

        }
    }


    /**
     * Removes an upVote/downVote from the user's line in the table if the correct
     * reaction is removed from their message.
     *
     * @param event the MessageReactionRemove event on a user's message
     */
    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        long messageAuthId = getMessageAuthId(event);
        if (event.getMember() == null || event.getUser() == null || event.getUser().isBot()) {
            return;
        }

        if (event.getReactionEmote().getIdLong() == Server.CHECK_EMOJI_ID) {
            // Leaving course channel
            if (event.getChannel().getIdLong() == Server.CHANNELS_CHANNEL_ID) {
                event.getTextChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
                    String content = message.getContentRaw();
                    Role role = event.getGuild().getRolesByName(content, false).get(0);
                    if (role != null) event.getGuild().removeRoleFromMember(event.getMember(), role).queue();
                });
            }
        }

        if (event.getUserIdLong() == getMessageAuthId(event)) {
            return;
        }

        try {
            // If upVote removed, remove upVote
            if (event.getReactionEmote().getEmote().getName().startsWith("upvote")) {
                if (!kc.userExists(messageAuthId)) {
                    kc.addUser(messageAuthId);
                }
                kc.updateUpVotes(messageAuthId, -1);
            }
            // If downVote removed, remove downVote
            if (event.getReactionEmote().getEmote().getName().startsWith("downvote")) {
                if (!kc.userExists(messageAuthId)) {
                    kc.addUser(messageAuthId);
                }
                kc.updateDownVotes(messageAuthId, -1);
            }
        } catch (Exception ignored) {}
    }
}

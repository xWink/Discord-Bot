package main.eventlisteners;

import database.connectors.KarmaConnector;
import main.Server;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.RoleAction;
import net.dv8tion.jda.internal.requests.restaction.operator.FlatMapRestAction;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

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
     * certain message reactions.
     *
     * @param event the MessageReactionAdd event on a user's message
     */
    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (event.getMember() == null || event.getUser() == null || event.getUser().isBot())
            return;

        if (event.getReactionEmote().getIdLong() == Server.CHECK_EMOJI_ID) {
            // User agrees to ToS
            if (event.getMessageIdLong() == Server.WELCOME_MESSAGE_ID)
                addToSRole(event);

                // Joining course channel
            else if (event.getChannel().getIdLong() == Server.CHANNELS_CHANNEL_ID)
                joinCourse(event);
        }

        if (event.getUserIdLong() == getMessageAuthId(event))
            return;

        handleKarmaAdd(event);
    }


    /**
     * Adds a a course-related role to a user given a specified reaction to a message in the courses channel.
     *
     * @param event the MessageReactionEvent that triggered the ReactionEventListener
     */
    private void joinCourse(@NotNull MessageReactionAddEvent event) {
        final Member member = event.getMember();
        final Guild guild = event.getGuild();

        if (member == null)
            return;

        event.getTextChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
            String content = message.getContentRaw().replace("*", "");
            List<Role> roles = guild.getRolesByName(content, true);

            if (roles.isEmpty()) {
                guild.createRole().setName(content)
                        .queue(role -> createChannel(member, guild, content, role));
                return;
            }

            guild.addRoleToMember(member, roles.get(0)).queue();
        });
    }


    private void createChannel(@NotNull Member member, @NotNull Guild guild, @NotNull String channelName, @NotNull Role role) {
        if (guild.getTextChannelsByName(channelName, true).isEmpty()) {
            guild.addRoleToMember(member, role).queue();
            return;
        }
        ArrayList<Permission> permissions = new ArrayList<>(Collections.singletonList(Permission.MESSAGE_READ));
        guild.getCategoriesByName("courses", false).get(0)
                .createTextChannel(channelName)
                .addPermissionOverride(
                        guild.getRolesByName("@everyone", false).get(0),
                        null,
                        permissions
                ).addPermissionOverride(
                        role,
                        permissions,
                        null
                ).queue(textChannel -> guild.addRoleToMember(member, role).queue());
    }


    /**
     * Adds upvotes to the user who received an upvote or a downvote if they received a downvote.
     *
     * @param event the MessageReactionEvent that triggered the ReactionEventListener
     */
    private void handleKarmaAdd(@NotNull MessageReactionAddEvent event) {
        long messageAuthId = getMessageAuthId(event);
        try {
            // Add upVotes
            if (event.getReactionEmote().getEmote().getName().startsWith("upvote")) {
                if (!kc.userExists(messageAuthId))
                    kc.addUser(messageAuthId);
                kc.updateUpVotes(messageAuthId, 1);
            }

            // Add downVotes
            if (event.getReactionEmote().getEmote().getName().startsWith("downvote")) {
                if (!kc.userExists(messageAuthId))
                    kc.addUser(messageAuthId);
                kc.updateDownVotes(messageAuthId, 1);
            }
        } catch (Exception ignored) {}
    }


    /**
     * Adds the ToS role to users who react to the Terms of Service agreement.
     * This role is needed to access the rest of the server.
     *
     * @param event the MessageReactionEvent that triggered the ReactionEventListener
     */
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
        if (event.getMember() == null || event.getUser() == null || event.getUser().isBot())
            return;

        if (event.getReactionEmote().getIdLong() == Server.CHECK_EMOJI_ID) {
            // Leaving course channel
            if (event.getChannel().getIdLong() == Server.CHANNELS_CHANNEL_ID)
                leaveCourseChannel(event);
        }

        if (event.getUserIdLong() == getMessageAuthId(event))
            return;

        handleKarmaRemove(event);
    }


    /**
     * Removes the role associated with a given course channel.
     *
     * @param event the MessageReactionRemove event that resulted in the user no longer having a reaction to
     *              the specific course channel message in the courses channel
     */
    private void leaveCourseChannel(MessageReactionRemoveEvent event) {
        if (event.getMember() == null)
            return;
        event.getTextChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
            String content = message.getContentRaw().replace("*", "");

            List<Role> roles = event.getGuild().getRolesByName(content, true);
            if (roles.isEmpty())
                return;

            Role role = event.getGuild().getRolesByName(content, true).get(0);
            if (role != null)
                event.getGuild().removeRoleFromMember(event.getMember(), role).queue();
        });
    }


    /**
     * Removes upvotes from the user who lost an upvote or a downvote if they lost a downvote.
     *
     * @param event the MessageReactionRemove event that resulted in the user no longer having a reaction to
     *              another user's message
     */
    private void handleKarmaRemove(MessageReactionRemoveEvent event) {
        long messageAuthId = getMessageAuthId(event);
        try {
            // If upVote removed, remove upVote
            if (event.getReactionEmote().getEmote().getName().startsWith("upvote")) {
                if (!kc.userExists(messageAuthId))
                    kc.addUser(messageAuthId);
                kc.updateUpVotes(messageAuthId, -1);
            }

            // If downVote removed, remove downVote
            if (event.getReactionEmote().getEmote().getName().startsWith("downvote")) {
                if (!kc.userExists(messageAuthId))
                    kc.addUser(messageAuthId);
                kc.updateDownVotes(messageAuthId, -1);
            }
        } catch (Exception ignored) {}
    }
}

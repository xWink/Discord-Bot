package main.eventlisteners;

import database.connectors.KarmaConnector;
import main.Server;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
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
        if (event.getMember() == null || event.getUser() == null)
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
     * Takes the message content of a message that was reacted to and assigns a role with the
     * same name as the message content to the user, such that the role has permission to view
     * a private channel with the same name. If either the role or the channel do not exist,
     * they are created.
     *
     * @param event the MessageReactionEvent that triggered the ReactionEventListener
     */
    private void joinCourse(@NotNull MessageReactionAddEvent event) {
        final Member member = event.getMember();
        final Guild guild = event.getGuild();

        if (member == null)
            return;

        AtomicReference<String> content = new AtomicReference<>();
        AtomicReference<Role> role = new AtomicReference<>();

        /*
         * 1. Get message that was reacted to
         * 2. Set message content to content variable
         * 3. Create/get corresponding role
         * 4. Set corresponding role to role variable
         * 5. Create/get corresponding channel
         * 6. If member is not bot, assign role to member
         */

        event.getTextChannel().retrieveMessageById(event.getMessageId()).submit()
                .thenAccept(message -> content.set(message.getContentRaw()))
                .thenCompose(aVoid -> getCorrespondingRole(guild, content.get()))
                .thenAccept(role::set)
                .thenCompose(aVoid -> getCorrespondingChannel(guild, content.get(), role.get()))
                .thenAccept(aVoid -> {
                    if (!member.getUser().isBot())
                        guild.addRoleToMember(member, role.get()).queue();
                })
                .join();
    }

    /**
     * Checks if a role with a given name exists in a given guild. If it doesn't, the
     * CompletableFuture of the creation of the role is returned. If it does, the
     * CompletableFuture of the role itself is returned.
     *
     * @param guild the Discord server to search for a role or to add a role to
     * @param roleName the name of the role to be found or created
     * @return the CompletableFuture that supplies a role which either already exists or is created
     */
    private CompletableFuture<Role> getCorrespondingRole(Guild guild, String roleName) {
        List<Role> roles = guild.getRolesByName(roleName, true);
        if (roles.isEmpty())
            return guild.createRole().setName(roleName).submit();
        return CompletableFuture.supplyAsync(() -> roles.get(0));
    }

    /**
     * Checks if a channel with a given name exists in a given guild. If it doesn't, the
     * CompletableFuture of the creation of the channel is returned. If it does, the
     * CompletableFuture of the channel itself is returned.
     *
     * @param guild the Discord server to search for a channel or to add a channel to
     * @param channelName the name of the channel to be found or created
     * @return the CompletableFuture that supplies a channel which either already exists or is created
     */
    private CompletableFuture<TextChannel> getCorrespondingChannel(Guild guild, String channelName, Role role) {
        channelName = channelName.replaceAll("\\*", "").replaceAll(" +-* *", "-").replaceAll(" ", "-");
        List<TextChannel> channels = guild.getTextChannelsByName(channelName, true);
        ArrayList<Permission> perms = new ArrayList<>(Collections.singletonList(Permission.MESSAGE_READ));

        // Create channel if not exists
        if (channels.isEmpty())
            return guild.getCategoriesByName("your channels", true).get(0)
                    .createTextChannel(channelName)
                    .addPermissionOverride(guild.getRolesByName("@everyone", false).get(0), null, perms)
                    .addPermissionOverride(role, perms, null).submit();

        // Add permission for new role to see channel if role not exists
        else if (channels.get(0).getPermissionOverride(role) == null)
            channels.get(0).createPermissionOverride(role).setAllow(perms).queue();

        return CompletableFuture.supplyAsync(() -> channels.get(0));
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
     * This role is needed to access the rest of the server. Once the user receives the role,
     * they are given a welcome message in the general channel and pinged in the channels channel.
     *
     * @param event the MessageReactionEvent that triggered the ReactionEventListener
     */
    private void addToSRole(MessageReactionAddEvent event) {
        Guild guild = event.getGuild();
        Role tosRole = Objects.requireNonNull(guild.getRoleById(Server.TOS_ROLE_ID));
        Member member = Objects.requireNonNull(event.getMember());

        if (member.getRoles().contains(tosRole)) {
            return;
        }
        TextChannel general = Objects.requireNonNull(guild.getTextChannelById(Server.GENERAL_CHANNEL_ID));
        TextChannel channels = Objects.requireNonNull(guild.getTextChannelById(Server.CHANNELS_CHANNEL_ID));
        guild.addRoleToMember(member, tosRole).queue();

        general.sendMessage(member.getAsMention() + " Welcome to the server! "
                + "Check out " + channels.getAsMention() + " to join course-related channels").queue();
        channels.sendMessage(member.getAsMention()).queue(message -> message.delete().queue());
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
        if (event.getReactionEmote().getIdLong() == Server.CHECK_EMOJI_ID)
            if (event.getChannel().getIdLong() == Server.CHANNELS_CHANNEL_ID) // Leaving course channel
                leaveCourseChannel(event);
        if (event.getUserIdLong() == getMessageAuthId(event))
            return;
        handleKarmaRemove(event);
    }


    /**
     * Removes the role associated with a given course channel.
     *
     * @param event the MessageReactionRemove event that resulted in the user no longer having a reaction to
     *              the specific course channel message in the channels channel
     */
    private void leaveCourseChannel(MessageReactionRemoveEvent event) {
        if (event.getMember() == null)
            return;
        event.getTextChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
            Guild guild = event.getGuild();
            String content = message.getContentRaw();

            List<Role> roles = guild.getRolesByName(content, true);
            if (roles.isEmpty())
                return;

            Role role = roles.get(0);
            if (role != null)
                guild.removeRoleFromMember(event.getMember(), role).queue();
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

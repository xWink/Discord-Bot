package integration;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.mockito.ArgumentMatchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A fake chat message. Run a command against it and read back what the bot
 * said, so command behaviour can be tested without logging in to Discord.
 */
public final class FakeDiscord {

    private static final AtomicLong IDS = new AtomicLong(System.currentTimeMillis() * 1000);

    public final List<String> texts = new ArrayList<>();
    public final List<MessageEmbed> embeds = new ArrayList<>();
    public final List<MessageEmbed> directMessages = new ArrayList<>();
    public final MessageReceivedEvent event;
    public final long userId;

    /** @return a stand-in for a Discord user, to mention in a message */
    @SuppressWarnings("unchecked")
    public static User user(long id, String name) {
        User u = mock(User.class);
        when(u.getIdLong()).thenReturn(id);
        when(u.getId()).thenReturn(Long.toString(id));
        when(u.getName()).thenReturn(name);
        when(u.getAsMention()).thenReturn("<@" + id + ">");
        when(u.isBot()).thenReturn(false);
        when(u.getAvatarUrl()).thenReturn("https://example.com/a.png");
        return u;
    }

    @SuppressWarnings("unchecked")
    public static FakeDiscord say(long userId, String name, String content, long channelId, User... mentions) {
        return new FakeDiscord(userId, name, content, channelId, mentions);
    }

    @SuppressWarnings("unchecked")
    private FakeDiscord(long userId, String name, String content, long channelId, User[] mentioned) {
        this.userId = userId;
        long messageId = IDS.incrementAndGet();

        MessageCreateAction action = mock(MessageCreateAction.class, RETURNS_SELF);

        MessageChannelUnion channel = mock(MessageChannelUnion.class);
        when(channel.getIdLong()).thenReturn(channelId);
        when(channel.getId()).thenReturn(Long.toString(channelId));
        when(channel.sendMessage(ArgumentMatchers.<CharSequence>any())).thenAnswer(inv -> {
            texts.add(inv.getArgument(0).toString());
            return action;
        });
        when(channel.sendMessageEmbeds(any(MessageEmbed.class), any(MessageEmbed[].class))).thenAnswer(inv -> {
            embeds.add(inv.getArgument(0));
            return action;
        });

        User author = user(userId, name);
        net.dv8tion.jda.api.requests.restaction.CacheRestAction<PrivateChannel> open = mock(net.dv8tion.jda.api.requests.restaction.CacheRestAction.class);
        PrivateChannel dm = mock(PrivateChannel.class);
        when(dm.sendMessageEmbeds(any(MessageEmbed.class), any(MessageEmbed[].class))).thenAnswer(inv -> {
            directMessages.add(inv.getArgument(0));
            return action;
        });
        org.mockito.Mockito.doAnswer(inv -> {
            ((Consumer<PrivateChannel>) inv.getArgument(0)).accept(dm);
            return null;
        }).when(open).queue(any(Consumer.class), any(Consumer.class));
        when(author.openPrivateChannel()).thenReturn(open);

        List<User> users = new ArrayList<>(Arrays.asList(mentioned));
        List<Member> members = new ArrayList<>();
        for (User u : users) {
            Member m = mock(Member.class);
            long mentionedId = u.getIdLong();
            when(m.getUser()).thenReturn(u);
            when(m.getIdLong()).thenReturn(mentionedId);
            members.add(m);
        }
        Mentions mentions = mock(Mentions.class);
        when(mentions.getUsers()).thenReturn(users);
        when(mentions.getMembers()).thenReturn(members);
        when(mentions.getRoles()).thenReturn(Collections.emptyList());

        RestAction<Void> reaction = mock(RestAction.class);
        Message message = mock(Message.class);
        when(message.getContentRaw()).thenReturn(content);
        when(message.getMentions()).thenReturn(mentions);
        when(message.getAuthor()).thenReturn(author);
        when(message.getIdLong()).thenReturn(messageId);
        when(message.getId()).thenReturn(Long.toString(messageId));
        when(message.getChannel()).thenReturn(channel);
        when(message.addReaction(any(Emoji.class))).thenReturn(reaction);
        when(message.isFromGuild()).thenReturn(true);

        Member self = mock(Member.class);
        when(self.getUser()).thenReturn(author);
        when(self.getRoles()).thenReturn(Collections.emptyList());
        when(self.getIdLong()).thenReturn(userId);
        Guild guild = mock(Guild.class);
        JDA jda = mock(JDA.class);

        event = mock(MessageReceivedEvent.class);
        when(event.getChannel()).thenReturn(channel);
        when(event.getAuthor()).thenReturn(author);
        when(event.getMessage()).thenReturn(message);
        when(event.getMessageId()).thenReturn(Long.toString(messageId));
        when(event.getMessageIdLong()).thenReturn(messageId);
        when(event.getMember()).thenReturn(self);
        when(event.getGuild()).thenReturn(guild);
        when(event.getJDA()).thenReturn(jda);
        when(event.isFromGuild()).thenReturn(true);
    }

    /** Everything the bot said in the channel, as plain text including embed contents. */
    public String said() {
        StringBuilder sb = new StringBuilder();
        for (String t : texts) sb.append(t).append('\n');
        for (MessageEmbed e : embeds) sb.append(flatten(e)).append('\n');
        for (MessageEmbed e : directMessages) sb.append("[DM] ").append(flatten(e)).append('\n');
        return sb.toString();
    }

    public static String flatten(MessageEmbed e) {
        StringBuilder sb = new StringBuilder();
        if (e.getTitle() != null) sb.append(e.getTitle()).append(" | ");
        if (e.getDescription() != null) sb.append(e.getDescription()).append(" | ");
        for (MessageEmbed.Field f : e.getFields()) sb.append(f.getName()).append(": ").append(f.getValue()).append(" | ");
        return sb.toString();
    }
}

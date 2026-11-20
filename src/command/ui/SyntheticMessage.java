package command.ui;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds a message event for text that no one typed: a slash command or a
 * button click rewritten as the equivalent {@code !command}. Commands read the
 * author, channel, guild, content and mentions, which are all real here.
 * Anything that acts on the message itself (a reaction, a delete) does nothing,
 * because there is no message on Discord to act on.
 */
public final class SyntheticMessage {

    private SyntheticMessage() { }

    /**
     * @param jda the bot
     * @param id the interaction ID, used as the message ID so receipts stay unique per click
     * @param content the text command, for example {@code !bet 5}
     * @param user who is acting
     * @param member their guild member, or null in a DM
     * @param channel where they acted
     * @param users users mentioned in the text
     * @param roles roles mentioned in the text
     * @param members members mentioned in the text
     * @return an event ready to hand to a command
     */
    public static MessageReceivedEvent event(JDA jda, long id, String content, User user, Member member,
                                             MessageChannelUnion channel, List<User> users, List<Role> roles,
                                             List<Member> members) {
        Mentions mentions = mentions(jda, users, roles, members);
        Message message = (Message) Proxy.newProxyInstance(Message.class.getClassLoader(),
                new Class<?>[]{Message.class}, new MessageHandler(jda, id, content, user, member, channel, mentions));
        return new MessageReceivedEvent(jda, id, message);
    }

    private static Mentions mentions(JDA jda, List<User> users, List<Role> roles, List<Member> members) {
        return (Mentions) Proxy.newProxyInstance(Mentions.class.getClassLoader(), new Class<?>[]{Mentions.class},
                (proxy, m, args) -> {
                    switch (m.getName()) {
                        case "getUsers": return new ArrayList<>(users);
                        case "getRoles": return new ArrayList<>(roles);
                        case "getMembers": return new ArrayList<>(members);
                        case "getJDA": return jda;
                        default: return Inert.defaultFor(m.getReturnType(), proxy, m);
                    }
                });
    }

    private static final class MessageHandler implements InvocationHandler {
        private final JDA jda;
        private final long id;
        private final String content;
        private final User user;
        private final Member member;
        private final MessageChannelUnion channel;
        private final Mentions mentions;

        MessageHandler(JDA jda, long id, String content, User user, Member member, MessageChannelUnion channel,
                       Mentions mentions) {
            this.jda = jda;
            this.id = id;
            this.content = content;
            this.user = user;
            this.member = member;
            this.channel = channel;
            this.mentions = mentions;
        }

        @Override
        public Object invoke(Object proxy, Method m, Object[] args) {
            switch (m.getName()) {
                case "getContentRaw":
                case "getContentDisplay":
                case "getContentStripped": return content;
                case "getAuthor": return user;
                case "getMember": return member;
                case "getChannel": return channel;
                case "getGuildChannel": return channel;
                case "getGuild": return member == null ? null : member.getGuild();
                case "getIdLong": return id;
                case "getId": return Long.toString(id);
                case "getMentions": return mentions;
                case "getJDA": return jda;
                case "isFromGuild": return member != null;
                case "isWebhookMessage": return false;
                case "getAttachments": case "getEmbeds": case "getReactions": return Collections.emptyList();
                case "toString": return "SyntheticMessage[" + content + "]";
                case "hashCode": return (int) id;
                case "equals": return proxy == args[0];
                default: return Inert.defaultFor(m.getReturnType(), proxy, m);
            }
        }
    }
}

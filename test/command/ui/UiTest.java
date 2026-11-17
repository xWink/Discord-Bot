package command.ui;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UiTest {

    @Test
    public void buttonIdsRoundTrip() {
        Buttons.Parsed p = Buttons.parseId(Buttons.mine(42L, "!contribute 5 confirm", "x",
                net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SUCCESS).getId());
        assertEquals(42L, p.allowed());
        assertTrue(p.consume());
        assertEquals("!contribute 5 confirm", p.text());
        assertNull(Buttons.parseId("help:1"));
        assertNull(Buttons.parseId("cmd:notanumber:x:!hit"));
    }

    @Test
    public void answerButtonsAreLettered() {
        assertEquals(4, Buttons.answers(0, false, "!answer", 4).size());
        assertEquals(5, Buttons.answers(0, false, "!answer", 6).size());
        assertEquals("!answer c", Buttons.parseId(((net.dv8tion.jda.api.interactions.components.buttons.Button)
                Buttons.answers(7, true, "!answer", 4).get(2)).getId()).text());
    }

    @Test
    public void everySlashCommandIsValidAndHasATextCommand() throws Exception {
        Set<String> keys = new HashSet<>(Arrays.asList("higher", "lower", "cashout"));
        Pattern keyPattern = Pattern.compile("super\\(\"!([a-z]+)\"");
        try (Stream<Path> files = Files.walk(Paths.get("src"))) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                Matcher m = keyPattern.matcher(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
                while (m.find()) keys.add(m.group(1));
            }
        }
        List<SlashCommandData> all = SlashCommands.all();
        assertTrue(all.size() <= 100);
        Set<String> seen = new HashSet<>();
        for (SlashCommandData d : all) {
            assertTrue(d.getName() + " is not a command", keys.contains(d.getName()));
            assertTrue("duplicate " + d.getName(), seen.add(d.getName()));
            assertTrue(d.getName().matches("[a-z0-9_-]{1,32}"));
            assertTrue(d.getDescription().length() <= 100);
        }
        assertTrue(seen.containsAll(Arrays.asList("bet", "wallet", "gift", "shop", "economy")));
    }

    @Test
    public void giftTranslatesToTheTextCommand() {
        User target = user(77L);
        SlashCommandInteractionEvent e = slash("gift", null, null);
        option(e, "who", mapping(target));
        option(e, "amount", mapping(5L));
        option(e, "note", mapping("happy  birthday"));
        SlashCommands.Translation t = SlashCommands.translate(e);
        assertEquals("!gift <@77> 5 wrap happy birthday", t.text);
        assertEquals(1, t.users.size());
    }

    @Test
    public void subcommandsAndFlagsTranslate() {
        SlashCommandInteractionEvent shop = slash("shop", null, "browse");
        assertEquals("!shop", SlashCommands.translate(shop).text);

        SlashCommandInteractionEvent buy = slash("shop", null, "buy");
        option(buy, "code", mapping("flair-day"));
        assertEquals("!shop buy flair-day", SlashCommands.translate(buy).text);

        SlashCommandInteractionEvent pay = slash("contribute", null, null);
        option(pay, "amount", mapping(10L));
        OptionMapping yes = mock(OptionMapping.class);
        when(yes.getAsBoolean()).thenReturn(true);
        option(pay, "confirm", yes);
        assertEquals("!contribute 10 confirm", SlashCommands.translate(pay).text);

        SlashCommandInteractionEvent goal = slash("economy", "goal", "new");
        option(goal, "target", mapping(20L));
        option(goal, "title", mapping("Trivia night"));
        assertEquals("!economy goal new 20 Trivia night", SlashCommands.translate(goal).text);

        assertEquals("!wallet", SlashCommands.translate(slash("wallet", null, null)).text);
    }

    @Test
    public void syntheticMessageCarriesWhatCommandsRead() {
        JDA jda = mock(JDA.class);
        User user = user(5L);
        Member member = mock(Member.class);
        MessageChannelUnion channel = mock(MessageChannelUnion.class);
        when(channel.getIdLong()).thenReturn(9L);
        User other = user(6L);
        MessageReceivedEvent ev = SyntheticMessage.event(jda, 123L, "!duel <@6> 3", user, member, channel,
                Collections.singletonList(other), Collections.<Role>emptyList(), Collections.<Member>emptyList());
        assertEquals("!duel <@6> 3", ev.getMessage().getContentRaw());
        assertEquals(5L, ev.getAuthor().getIdLong());
        assertEquals(9L, ev.getChannel().getIdLong());
        assertEquals("123", ev.getMessageId());
        assertEquals(6L, ev.getMessage().getMentions().getUsers().get(0).getIdLong());
        assertTrue(ev.getMessage().getMentions().getRoles().isEmpty());
        // acting on the message itself must be harmless
        ev.getMessage().addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("✅")).queue();
        ev.getMessage().delete().queue();
    }

    private static User user(long id) {
        User u = mock(User.class);
        when(u.getIdLong()).thenReturn(id);
        when(u.getAsMention()).thenReturn("<@" + id + ">");
        return u;
    }

    private static OptionMapping mapping(User u) {
        OptionMapping m = mock(OptionMapping.class);
        when(m.getAsUser()).thenReturn(u);
        return m;
    }

    private static OptionMapping mapping(long n) {
        OptionMapping m = mock(OptionMapping.class);
        when(m.getAsLong()).thenReturn(n);
        return m;
    }

    private static OptionMapping mapping(String s) {
        OptionMapping m = mock(OptionMapping.class);
        when(m.getAsString()).thenReturn(s);
        return m;
    }

    private static SlashCommandInteractionEvent slash(String name, String group, String sub) {
        SlashCommandInteractionEvent e = mock(SlashCommandInteractionEvent.class);
        when(e.getName()).thenReturn(name);
        when(e.getSubcommandGroup()).thenReturn(group);
        when(e.getSubcommandName()).thenReturn(sub);
        return e;
    }

    private static void option(SlashCommandInteractionEvent e, String name, OptionMapping m) {
        when(e.getOption(name)).thenReturn(m);
    }
}

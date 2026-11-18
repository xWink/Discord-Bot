package command.ui;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every slash command, and how to turn one into the text command it stands for.
 * {@code /bet amount:5} becomes {@code !bet 5}. Subcommand and group names are
 * written out in order, then each option's value in the order it is declared.
 * A true yes/no option adds its own name ({@code confirm}); an option listed in
 * {@link #LITERALS} adds a fixed word before its value.
 */
public final class SlashCommands {

    /** Subcommands whose name is not part of the text command ({@code /shop browse} is just {@code !shop}). */
    private static final Set<String> SILENT = new HashSet<>();
    /** Words that go before an option's value, keyed {@code command/sub/option}. */
    private static final Map<String, String> LITERALS = new LinkedHashMap<>();
    private static final Map<String, SlashCommandData> DATA = new LinkedHashMap<>();

    static {
        SILENT.add("shop/browse");
        SILENT.add("alumni/status");
        LITERALS.put("gift/note", "wrap");
        LITERALS.put("profile/animation", "gif");
        build();
    }

    private SlashCommands() { }

    /** @return the definitions to register with Discord */
    public static List<SlashCommandData> all() {
        return new ArrayList<>(DATA.values());
    }

    // --- definitions ---------------------------------------------------------------------------------------

    private static void build() {
        int maxStake = 1000000;
        String[] letters = {"A", "B", "C", "D", "E", "F"};

        // Everyone, no options
        simple("achievements", "Your unlocked achievements");
        simple("arcade", "Today's arcade puzzle");
        simple("arcadescores", "This week's arcade leaderboard");
        simple("bang", "Russian roulette. Your first pull each day pays your daily");
        simple("bangscore", "Bang rankings");
        simple("bangweek", "This week's bang rankings and best streaks");
        simple("mybang", "Your bang numbers");
        simple("daily", "When your daily reward is ready");
        simple("wallet", "Your balance, receipts and decay (sent by DM)");
                simple("karma", "Your karma");
        simple("ping", "Your latency");
        simple("hit", "Blackjack: take a card");
        simple("stand", "Blackjack: stand");
        simple("myhand", "Blackjack: show your hand");
        simple("higher", "Higher or Lower: guess higher");
        simple("lower", "Higher or Lower: guess lower");
        simple("cashout", "Higher or Lower: take your winnings");
        simple("accept", "Accept a duel challenge");
        simple("decline", "Decline a duel challenge");
        simple("duelboard", "Duel ratings");
        simple("market", "Colour rentals and permanent colours");
        simple("goal", "The community goal");
        simple("coop", "Weekend server-wide arcade target");

        // Everyone, with options
        add(Commands.slash("profile", "Your profile card").addOptions(
                choice("animation", "Turn the animated card on or off", false, "on", "off")));
        add(Commands.slash("help", "The command list").addOptions(choice("topic", "Which page to open", false,
                "overview", "games", "arcade", "economy", "profile", "info", "moderators")));
        add(Commands.slash("bet", "Play blackjack").addOptions(num("amount", "Stake in GC", true, 1, maxStake)));
        add(Commands.slash("flip", "Flip a coin. Call it and add a stake to bet").addOptions(
                choice("call", "Heads or tails", false, "heads", "tails"), num("stake", "Stake in GC", false, 1, maxStake)));
        add(Commands.slash("slots", "Spin the reels").addOptions(num("stake", "Stake in GC", true, 1, maxStake)));
        add(Commands.slash("roulette", "Spin the wheel").addOptions(
                choice("bet", "What to bet on", true, "red", "black", "odd", "even", "low", "high", "green"),
                num("stake", "Stake in GC", true, 1, maxStake)));
        add(Commands.slash("crash", "Pick a multiplier to cash out at").addOptions(
                num("stake", "Stake in GC", true, 1, maxStake), str("target", "Multiplier to cash out at, like 2.5", false)));
        add(Commands.slash("scratch", "Scratch card").addOptions(num("stake", "Stake in GC", true, 1, maxStake)));
        add(Commands.slash("mines", "A 4x4 board of buttons. Uncover gems, cash out before a mine").addOptions(
                num("stake", "Stake in GC", true, 1, maxStake), num("mines", "How many mines, 1 to 8", false, 1, 8)));
        add(Commands.slash("wordle", "The daily five-letter word. Leave the word off to see your board").addOptions(
                str("word", "Your guess", false)));
        add(Commands.slash("race", "Put up a stake; the first right answer wins it").addOptions(
                num("stake", "Stake in GC", true, 1, maxStake), str("category", "Trivia category", false)));
        add(Commands.slash("coinduel", "Coin flip against a player for equal stakes").addOptions(
                user("opponent", "Who to challenge", true), num("stake", "Stake each, in GC", true, 1, maxStake)));
        add(Commands.slash("hilo", "Higher or Lower").addOptions(num("stake", "Stake in GC", true, 1, maxStake)));
        add(Commands.slash("dice", "Two dice. Leave the stake off to play free").addOptions(
                choice("pick", "What you are betting on", true, "low", "high", "seven"),
                num("stake", "Stake in GC", false, 1, maxStake)));
        add(Commands.slash("paidplay", "Turn games that risk coins on or off for you")
                .addOptions(choice("setting", "On or off", false, "on", "off")));
        add(Commands.slash("duel", "Challenge someone to a trivia duel").addOptions(
                user("opponent", "Who to challenge", true), num("stake", "Stake each, 0 for free", false, 0, maxStake)));
        add(Commands.slash("duelanswer", "Answer your duel question").addOptions(choice("letter", "Your answer", true, letters)));
        add(Commands.slash("answer", "Answer your puzzle or quiz question").addOptions(choice("letter", "Your answer", true, letters)));
        add(Commands.slash("panswer", "Join a hosted trivia round").addOptions(choice("letter", "Your answer", true, letters)));
        add(Commands.slash("weekly", "Weekly 3-question challenge").addOptions(
                choice("kind", "Which challenge", true, "code", "trivia")));
        add(Commands.slash("gauntlet", "3 mixed rounds").addOptions(
                choice("mode", "Leave off for free practice", false, "prize")));
        add(Commands.slash("ptrivia", "Host a trivia round for the channel").addOptions(
                str("category", "Trivia category", true), num("cost", "What you pay, 5 to 10 GC", false, 5, 10)));
        add(Commands.slash("gift", "Send GryphCoins").addOptions(user("who", "Who gets them", true),
                num("amount", "How many GC", true, 1, 1000000),
                str("note", "Adds a gift card with this note for 3 GC extra", false)));
        add(Commands.slash("buy", "Buy a colour from the market").addOptions(num("number", "Item number from /market", true, 1, 100)));
        add(Commands.slash("swap", "Change colour for 5 GC and keep your time").addOptions(str("colour", "Colour name", true)));
        add(Commands.slash("installment", "Pay for a permanent colour in parts").addSubcommands(
                new SubcommandData("start", "Start a plan").addOptions(str("colour", "Colour name", true)),
                new SubcommandData("pay", "Pay toward your plan").addOptions(num("amount", "How many GC", true, 1, 1000000),
                        bool("confirm", "Go ahead and charge")),
                new SubcommandData("finish", "Deliver a fully paid colour")));
        add(Commands.slash("shop", "Cosmetics").addSubcommands(
                new SubcommandData("browse", "See the shop"),
                new SubcommandData("buy", "Buy something").addOptions(str("code", "Item code", true)),
                new SubcommandData("owned", "What you own")));
        add(Commands.slash("equip", "Wear a cosmetic you own").addOptions(str("code", "Item code, or none", true)));
        add(Commands.slash("banner", "Team banners").addSubcommands(
                new SubcommandData("request", "Sponsor a banner for a team").addOptions(role("team", "The team's role", true)),
                new SubcommandData("approve", "Approve a request (team members)").addOptions(num("id", "Request number", true, 1, 1000000)),
                new SubcommandData("decline", "Decline a request").addOptions(num("id", "Request number", true, 1, 1000000))));
        add(Commands.slash("contribute", "Fund the community goal").addOptions(
                num("amount", "How many GC", true, 1, 1000000), bool("confirm", "Go ahead and charge")));
        add(Commands.slash("bounty", "Jobs for coins").addSubcommands(
                new SubcommandData("list", "Open bounties"),
                new SubcommandData("post", "Post a bounty").addOptions(num("reward", "GC held until paid", true, 1, 1000),
                        str("description", "What needs doing", true)),
                new SubcommandData("claim", "Claim a bounty").addOptions(num("id", "Bounty number", true, 1, 1000000)),
                new SubcommandData("accept", "Pay the worker").addOptions(num("id", "Bounty number", true, 1, 1000000)),
                new SubcommandData("cancel", "Cancel your unclaimed bounty").addOptions(num("id", "Bounty number", true, 1, 1000000))));
        add(Commands.slash("event", "Host an event and confirm attendance").addSubcommands(
                new SubcommandData("start", "Start an event").addOptions(str("name", "Event name", true)),
                new SubcommandData("confirm", "Confirm who came").addOptions(user("player1", "Attendee", true),
                        user("player2", "Attendee", false), user("player3", "Attendee", false),
                        user("player4", "Attendee", false), user("player5", "Attendee", false)),
                new SubcommandData("end", "End the event and pay out")));
        add(Commands.slash("board", "Leaderboards").addOptions(
                choice("board", "Which board", true, economy.SeasonService.BOARDS)));
        add(Commands.slash("optout", "Hide yourself from public boards").addOptions(choice("setting", "On or off", false, "on", "off")));
        add(Commands.slash("alumni", "Alumni status").addSubcommands(
                new SubcommandData("status", "Your alumni status"),
                new SubcommandData("list", "Everyone who is alumni"),
                new SubcommandData("grant", "Bestow alumni (mods)").addOptions(user("who", "Member", true),
                        num("year", "Class year", false, 1900, 2100), str("note", "Thank-you note", false)),
                new SubcommandData("revoke", "Remove alumni (mods)").addOptions(user("who", "Member", true))));
        add(Commands.slash("id", "Look up a user").addOptions(str("discord_id", "Discord ID", true)));
        add(Commands.slash("info", "Course info, or search by title").addOptions(
                str("course", "A code like CIS1300, words from the title, or a department", true).setAutoComplete(true)));
        add(Commands.slash("prereq", "Draw what a course needs").addOptions(
                str("course", "A course code like CIS3750", true).setAutoComplete(true)));
        add(Commands.slash("join", "Join a course channel").addOptions(str("course", "Course code", true)));
        add(Commands.slash("leave", "Leave a course channel").addOptions(str("course", "Course code", true)));

        // Moderators. The command itself checks the role, so anyone else is told no.
        add(Commands.slash("economy", "Moderator tools for the economy")
                .addSubcommandGroups(new SubcommandGroupData("goal", "Community goals").addSubcommands(
                        new SubcommandData("new", "Start a goal").addOptions(num("target", "GC needed", true, 1, 1000000),
                                str("title", "Goal title", true)),
                        new SubcommandData("cancel", "Cancel the current goal")))
                .addSubcommands(
                        new SubcommandData("receipt", "Look up a wallet change").addOptions(str("id", "Receipt ID", true)),
                        new SubcommandData("adjust", "Documented correction").addOptions(user("who", "Member", true),
                                num("amount", "Plus or minus GC", true, -1000000, 1000000), str("note", "Why", true)),
                        new SubcommandData("decay", "Publish the first decay cutoff").addOptions(
                                num("weeks", "Weeks from now, at least 1", true, 1, 52))));
    }

    private static void simple(String name, String description) {
        add(Commands.slash(name, description));
    }

    private static void add(SlashCommandData data) {
        DATA.put(data.getName(), data);
    }

    private static OptionData num(String name, String desc, boolean required, long min, long max) {
        return new OptionData(OptionType.INTEGER, name, desc, required).setRequiredRange(min, max);
    }

    private static OptionData str(String name, String desc, boolean required) {
        return new OptionData(OptionType.STRING, name, desc, required);
    }

    private static OptionData user(String name, String desc, boolean required) {
        return new OptionData(OptionType.USER, name, desc, required);
    }

    private static OptionData role(String name, String desc, boolean required) {
        return new OptionData(OptionType.ROLE, name, desc, required);
    }

    private static OptionData bool(String name, String desc) {
        return new OptionData(OptionType.BOOLEAN, name, desc, false);
    }

    private static OptionData choice(String name, String desc, boolean required, String... values) {
        OptionData o = new OptionData(OptionType.STRING, name, desc, required);
        List<Choice> choices = new ArrayList<>();
        for (String v : values) choices.add(new Choice(v, v.toLowerCase()));
        return o.addChoices(choices);
    }

    // --- turning an invocation into text -------------------------------------------------------------------

    /** What one slash invocation says, as text plus the people and roles it names. */
    public static final class Translation {
        public final String text;
        public final List<net.dv8tion.jda.api.entities.User> users = new ArrayList<>();
        public final List<net.dv8tion.jda.api.entities.Member> members = new ArrayList<>();
        public final List<net.dv8tion.jda.api.entities.Role> roles = new ArrayList<>();

        Translation(String text) {
            this.text = text;
        }
    }

    /**
     * @param e the slash command
     * @return the equivalent text command, for example {@code !gift <@1> 5 wrap thanks}
     */
    public static Translation translate(SlashCommandInteractionEvent e) {
        String name = e.getName();
        String group = e.getSubcommandGroup();
        String sub = e.getSubcommandName();
        List<OptionData> declared = declaredOptions(name, group, sub);

        StringBuilder text = new StringBuilder("!").append(name);
        if (group != null) text.append(' ').append(group);
        String path = name + "/" + (sub == null ? "" : sub);
        if (sub != null && !SILENT.contains(path)) text.append(' ').append(sub);

        Translation t = new Translation(null);
        List<String> tokens = new ArrayList<>();
        for (OptionData d : declared) {
            OptionMapping m = e.getOption(d.getName());
            if (m == null) continue;
            String literal = LITERALS.get(name + "/" + d.getName());
            switch (d.getType()) {
                case BOOLEAN:
                    if (m.getAsBoolean()) tokens.add(d.getName());
                    break;
                case USER:
                    tokens.add(m.getAsUser().getAsMention());
                    t.users.add(m.getAsUser());
                    if (m.getAsMember() != null) t.members.add(m.getAsMember());
                    break;
                case ROLE:
                    tokens.add(m.getAsRole().getAsMention());
                    t.roles.add(m.getAsRole());
                    break;
                case INTEGER:
                    if (literal != null) tokens.add(literal);
                    tokens.add(Long.toString(m.getAsLong()));
                    break;
                default:
                    if (literal != null) tokens.add(literal);
                    tokens.add(m.getAsString().replaceAll("\\s+", " ").trim());
            }
        }
        for (String token : tokens) text.append(' ').append(token);
        Translation out = new Translation(text.toString());
        out.users.addAll(t.users);
        out.members.addAll(t.members);
        out.roles.addAll(t.roles);
        return out;
    }

    private static List<OptionData> declaredOptions(String name, String group, String sub) {
        SlashCommandData data = DATA.get(name);
        if (data == null) return Collections.emptyList();
        if (sub == null) return data.getOptions();
        List<SubcommandData> subs = data.getSubcommands();
        if (group != null) {
            for (SubcommandGroupData g : data.getSubcommandGroups()) {
                if (g.getName().equals(group)) subs = g.getSubcommands();
            }
        }
        for (SubcommandData s : subs) {
            if (s.getName().equals(sub)) return s.getOptions();
        }
        return Collections.emptyList();
    }
}

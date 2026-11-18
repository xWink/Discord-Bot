package command.commands;

import command.Command;
import main.Server;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.awt.Color;

/**
 * {@code !help} shows a menu: pick a category from the drop-down and the embed
 * switches to that category's commands. {@code !help games} (or any category
 * name) opens a page directly.
 */
public class Help extends Command {

    /** A command line on a help page. */
    private static final class Entry {
        final String usage;
        final String what;

        Entry(String usage, String what) {
            this.usage = usage;
            this.what = what;
        }
    }

    /** One page of the help menu. */
    public enum Category {
        OVERVIEW("overview", "📖", "Overview", "Start here", new Color(0xF5C542)),
        GAMES("games", "🎲", "Games", "Risk a few coins, or play free", new Color(0xE0457B),
                new Entry("!bet <amount>", "Blackjack. Pays 3:2, a tie returns your stake"),
                new Entry("!hit  !stand  !myhand", "Play your blackjack hand"),
                new Entry("!dice <low|high|seven> [stake]", "Two dice. Leave the stake off to play free"),
                new Entry("!slots <stake>", "Three reels, animated. Pairs pay 1.4x, triples up to 50x"),
                new Entry("!roulette <bet> <stake>", "Spin the wheel. red, black, odd, even, low, high pay 1:1; a number pays 35:1"),
                new Entry("!crash <stake> [target]", "Pick a multiplier to cash out at and watch it climb. Beat the crash to win"),
                new Entry("!scratch <stake>", "Scratch card. Match three to win, up to 50x"),
                new Entry("!mines <stake> [mines]", "A 4x4 board of buttons. Uncover gems, cash out before a mine"),
                new Entry("!coinduel @user <stake>", "Coin flip against a player for equal stakes. They press Accept"),
                new Entry("!race <stake> [category]", "Put up a stake on a trivia question; the first right answer wins it"),
                new Entry("!wordle [word]", "The daily five-letter word. Six guesses, +3 GC and a streak for solving it"),
                new Entry("!hilo <stake>", "Higher or Lower. Then !higher, !lower or !cashout"),
                new Entry("!duel @user [stake]", "Trivia duel. They reply !accept or !decline"),
                new Entry("!duelanswer <letter>", "Answer your duel question"),
                new Entry("!duelboard", "Duel ratings"),
                new Entry("!bang", "Russian roulette. Your first pull of the day pays your daily"),
                new Entry("!flip [heads|tails] [stake]", "Spinning coin. Call it and add a stake to bet 1:1"),
                new Entry("!paidplay [on|off]", "Turn games that risk coins on or off for you")),
        ARCADE("arcade", "🧠", "Arcade & quizzes", "Free daily puzzles that pay", new Color(0x3BA55D),
                new Entry("!arcade", "Today's puzzle. Your first round each day pays your daily"),
                new Entry("!answer <letter>", "Answer your puzzle or quiz question"),
                new Entry("!arcadescores", "This week's arcade leaderboard"),
                new Entry("!weekly <code|trivia>", "Weekly 3-question challenge, 10-15 GC once a week"),
                new Entry("!gauntlet [prize]", "3 mixed rounds. 'prize' costs 3 GC and wins the weekly badge"),
                new Entry("!coop", "Weekend server-wide target"),
                new Entry("!ptrivia <category> [5-10]", "Host a trivia round for the channel"),
                new Entry("!panswer <letter>", "Join a hosted round for free")),
        ECONOMY("economy", "💰", "Wallet & shop", "Earn, spend and send GryphCoins", new Color(0xFAA61A),
                new Entry("!wallet", "Your balance, reserved coins, decay and receipts (sent by DM)"),
                new Entry("!daily", "When your daily reward is ready"),
                new Entry("!gift @user <amount> [wrap note]", "Send coins. 'wrap' adds a card for 3 GC extra"),
                new Entry("!market  !buy <#>", "Colour rentals and permanent colours"),
                new Entry("!swap <colour>", "Change colour for 5 GC and keep your time"),
                new Entry("!installment", "Pay for a permanent colour in parts"),
                new Entry("!shop", "Cosmetics. !shop buy <code>, !shop owned"),
                new Entry("!equip <code|none>", "Wear a cosmetic you own (effects, titles, flair)"),
                new Entry("!banner request @TeamRole", "Sponsor a team banner for a week"),
                new Entry("!goal  !contribute <amount>", "Community goal and funding it"),
                new Entry("!bounty", "Post, claim and pay jobs for coins"),
                new Entry("!event", "Host or confirm event attendance")),
        PROFILE("profile", "🏆", "Profile & rankings", "Stats, boards and achievements", new Color(0x5865F2),
                new Entry("!profile", "Your stats, cosmetics and wallet"),
                new Entry("!profile gif on|off", "Animate your profile card (the mascot bobs and sparkles)"),
                new Entry("!achievements", "Your unlocks"),
                new Entry("!alumni  !alumni list", "Alumni status and the alumni list"),
                new Entry("!board <arcade|duel|collection|community>", "Leaderboards and seasons"),
                new Entry("!bangscore  !bangweek  !mybang", "Bang rankings and your numbers"),
                new Entry("!optout [on|off]", "Hide yourself from public boards"),
                new Entry("!karma", "Your karma")),
        INFO("info", "ℹ️", "Info & courses", "Utilities and course channels", new Color(0x99AAB5),
                new Entry("!ping", "Your latency"),
                new Entry("!id <Discord ID>", "Look up a user"),
                new Entry("!info <course | words | dept>", "Course info: a code, words from the title, or a department"),
                new Entry("!prereq <course>", "Draws the chain of prerequisites"),
                new Entry("!remind <when> <what>", "A reminder by DM: 2h, tomorrow, friday 5pm, 2026-12-04"),
                new Entry("!reminders", "Your waiting reminders"),
                new Entry("!dates [words]", "Next academic calendar dates, with a reminder button"),
                new Entry("!rate <course> <difficulty> <workload> [comment]", "Rate a course 1-5, anonymously"),
                new Entry("!reviews <course>", "How students rated a course, as a chart, with comments"),
                new Entry("!completed add <courses>", "List the courses you have finished"),
                new Entry("!next [dept] [level]", "Courses whose prerequisites you meet"),
                new Entry("!whynot <course>", "What is missing before you can take a course"),
                new Entry("!roles  !join  !leave", "Course channels")),
        MODS("moderators", "🛡️", "Moderators", "Staff only", new Color(0xED4245),
                new Entry("!economy receipt <id>", "Look up any wallet change"),
                new Entry("!economy adjust @user <+/-amount> <note>", "Documented correction"),
                new Entry("!economy goal new <target> <title>  /  cancel", "Community goals"),
                new Entry("!economy decay <weeks>", "Publish the first decay cutoff"),
                new Entry("!event start <name>", "Start an event without a pass"),
                new Entry("!alumni grant @user [year] [note]", "Bestow alumni. The year is optional. Adds the Alumni role if it exists"),
                new Entry("!alumni revoke @user", "Remove alumni"),
                new Entry("!banner approve|decline <id>", "Approve team banners"));

        final String key;
        final String emoji;
        final String label;
        final String blurb;
        final Color color;
        private final Entry[] entries;

        Category(String key, String emoji, String label, String blurb, Color color, Entry... entries) {
            this.key = key;
            this.emoji = emoji;
            this.label = label;
            this.blurb = blurb;
            this.color = color;
            this.entries = entries;
        }

        public static Category find(String text) {
            for (Category c : values()) {
                if (c.key.equalsIgnoreCase(text) || c.label.toLowerCase().startsWith(text.toLowerCase())) return c;
            }
            return null;
        }

        /** The embed for this page. */
        public EmbedBuilder page() {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(color);
            eb.setTitle(emoji + "  " + label);
            if (this == OVERVIEW) {
                StringBuilder sb = new StringBuilder("Pick a category from the menu below.\n\n");
                for (Category c : values()) {
                    if (c != OVERVIEW) sb.append(c.emoji).append("  **").append(c.label).append("** — ")
                            .append(c.blurb).append('\n');
                }
                eb.setDescription(sb.toString());
                eb.addField("Slash commands & buttons", "Every command also works as a `/command`, and games "
                        + "have buttons (Hit, Stand, Higher, A-D answers, Accept, Confirm).", false);
                eb.addField("New here?", "`!wallet` to see your coins, `!arcade` for a free puzzle, "
                        + "`!bet 2` to try blackjack.", false);
            } else {
                StringBuilder sb = new StringBuilder();
                for (Entry e : entries) sb.append("`").append(e.usage).append("`\n└ ").append(e.what).append("\n\n");
                eb.setDescription(sb.toString());
            }
            eb.setFooter("<needed> [optional] · most commands work in the bots or spam channel", null);
            return eb;
        }
    }

    public Help() {
        super("!help", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith("!help");
    }

    /**
     * Builds the drop-down that switches help pages. The owner's ID is part of
     * the component ID, so only they can change their menu.
     */
    public static StringSelectMenu menu(long ownerId, Category current) {
        StringSelectMenu.Builder b = StringSelectMenu.create("help:" + ownerId);
        for (Category c : Category.values()) {
            if (c == Category.MODS) continue;
            b.addOption(c.label, c.key, c.blurb, Emoji.fromUnicode(c.emoji));
        }
        b.addOption(Category.MODS.label, Category.MODS.key, Category.MODS.blurb, Emoji.fromUnicode(Category.MODS.emoji));
        b.setDefaultValues(current.key);
        return b.build();
    }

    @Override
    public void start(MessageReceivedEvent event) {
        if (event.getChannel().getIdLong() != Server.BOTS_CHANNEL_ID
                && event.getChannel().getIdLong() != Server.SPAM_CHANNEL_ID) {
            TextChannel bots = Server.API.getTextChannelById(Server.BOTS_CHANNEL_ID);
            if (bots != null) {
                event.getChannel().sendMessage("Say `!help` in " + bots.getAsMention() + " to see available commands!").queue();
            }
            return;
        }
        String[] a = event.getMessage().getContentRaw().split("\\s+");
        Category category = a.length >= 2 ? Category.find(a[1]) : Category.OVERVIEW;
        if (category == null) category = Category.OVERVIEW;
        event.getChannel().sendMessageEmbeds(category.page().build())
                .setActionRow(menu(event.getAuthor().getIdLong(), category)).queue();
    }
}

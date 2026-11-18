package command.commands.economy;

import command.Command;
import command.ui.Buttons;
import command.ui.Reveal;
import command.util.art.GameArt;
import database.Database;
import economy.EconomyService;
import economy.Wagers;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A coin flip between two players for equal stakes. {@code !coinduel @user <stake>} challenges; they press Accept
 * (or say {@code !coinaccept}) within a minute. Both stakes are held, a spinning coin decides, and the winner takes
 * the loser's stake. Heads goes to the challenger, tails to the opponent. Nothing is taken as a fee.
 */
public final class CoinDuel {

    public static final long LIFETIME_MS = 60 * 1000;

    private static final Random RANDOM = new Random();
    /** Open challenges by the opponent they are waiting on. */
    private static final ConcurrentMap<Long, Challenge> PENDING = new ConcurrentHashMap<>();

    private CoinDuel() { }

    static final class Challenge {
        final long challenger;
        final long opponent;
        final int stake;
        final long createdAt = System.currentTimeMillis();

        Challenge(long challenger, long opponent, int stake) {
            this.challenger = challenger;
            this.opponent = opponent;
            this.stake = stake;
        }

        boolean expired() {
            return System.currentTimeMillis() - createdAt > LIFETIME_MS;
        }
    }

    /** {@code !coinduel @user <stake>}. */
    public static class Start extends Command {

        public Start() {
            super("!coinduel", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().startsWith(getKey());
        }

        @Override
        public void start(MessageReceivedEvent event) {
            long me = event.getAuthor().getIdLong();
            String[] args = event.getMessage().getContentRaw().trim().split("\\s+");
            List<net.dv8tion.jda.api.entities.User> mentioned = event.getMessage().getMentions().getUsers();
            int stake = 0;
            try {
                if (args.length == 3) stake = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                stake = 0;
            }
            if (mentioned.size() != 1 || stake < 1) {
                event.getChannel().sendMessage("Say `!coinduel @user <stake>`. A coin flip for equal stakes: heads is you, "
                        + "tails is them. They have a minute to accept.").queue();
                return;
            }
            long them = mentioned.get(0).getIdLong();
            try {
                if (them == me || mentioned.get(0).isBot()) {
                    event.getChannel().sendMessage("Pick another player to challenge.").queue();
                    return;
                }
                Challenge existing = PENDING.get(them);
                if (existing != null && !existing.expired()) {
                    event.getChannel().sendMessage("They already have a coin duel waiting.").queue();
                    return;
                }
                String blocked = Wagers.blocked(me, stake);
                if (blocked != null) {
                    event.getChannel().sendMessage(blocked).queue();
                    return;
                }
                PENDING.put(them, new Challenge(me, them, stake));
                List<ItemComponent> buttons = new ArrayList<>();
                buttons.add(Buttons.of(them, true, "!coinaccept", "Accept", ButtonStyle.SUCCESS));
                buttons.add(Buttons.of(them, true, "!coindecline", "Decline", ButtonStyle.DANGER));
                event.getChannel().sendMessage("<@" + me + "> challenges <@" + them + "> to a coin duel for **" + stake
                        + " GC** each. Heads is " + event.getAuthor().getName() + ", tails is you. Maximum loss: "
                        + stake + " GC. Accept within a minute.").setActionRow(buttons).queue();
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }

    /** {@code !coinaccept} and {@code !coindecline}, from the player who was challenged. */
    public static class Reply extends Command {

        private final boolean accept;

        public Reply(String key) {
            super(key, true);
            this.accept = key.equals("!coinaccept");
        }

        @Override
        public void start(MessageReceivedEvent event) {
            long me = event.getAuthor().getIdLong();
            Challenge c = PENDING.get(me);
            if (c == null || c.expired()) {
                PENDING.remove(me, c);
                event.getChannel().sendMessage("You have no coin duel waiting.").queue();
                return;
            }
            if (!PENDING.remove(me, c)) return; // someone else's click got there first
            if (!accept) {
                event.getChannel().sendMessage("<@" + c.opponent + "> declined the coin duel.").queue();
                return;
            }
            try {
                String wait = Wagers.cooldown(me);
                if (wait != null) {
                    event.getChannel().sendMessage(wait).queue();
                    return;
                }
                for (long user : new long[]{c.challenger, c.opponent}) {
                    String blocked = Wagers.blocked(user, c.stake);
                    if (blocked != null) {
                        event.getChannel().sendMessage("The coin duel is off. <@" + user + ">: " + blocked).queue();
                        return;
                    }
                }
                boolean heads = RANDOM.nextBoolean();
                long winner = heads ? c.challenger : c.opponent;
                long loser = heads ? c.opponent : c.challenger;
                String id = event.getMessageId();
                try {
                    Database.inTransaction(conn -> {
                        EconomyService.reserveIn(conn, c.challenger, c.stake, "coinduel", id, "coinduel:" + id + ":c");
                        EconomyService.reserveIn(conn, c.opponent, c.stake, "coinduel", id, "coinduel:" + id + ":o");
                        String winRes = "coinduel:" + id + (heads ? ":c" : ":o");
                        String loseRes = "coinduel:" + id + (heads ? ":o" : ":c");
                        EconomyService.settleReservationIn(conn, winRes, c.stake, "coinduel");
                        EconomyService.settleReservationIn(conn, loseRes, -c.stake, "coinduel");
                        return null;
                    });
                } catch (EconomyService.InsufficientFundsRuntime e) {
                    event.getChannel().sendMessage("The coin duel is off: one of you cannot cover " + c.stake + " GC.").queue();
                    return;
                }
                String text = economy.Cosmetics.winHeader(winner) + "**" + (heads ? "Heads" : "Tails") + "!** <@" + winner
                        + "> wins **" + c.stake + " GC** from <@" + loser + ">. Maximum loss was " + c.stake + " GC.";
                Reveal.send(event.getChannel().sendFiles(FileUpload.fromData(GameArt.coinSpin(heads), "coin.gif"))
                                .setContent("The coin is in the air…"), event.getChannel(), text,
                        Buttons.single(Buttons.of(c.challenger, true, "!coinduel <@" + c.opponent + "> " + c.stake,
                                "Rematch (" + c.stake + " GC)", ButtonStyle.PRIMARY)), 1500);
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }
}

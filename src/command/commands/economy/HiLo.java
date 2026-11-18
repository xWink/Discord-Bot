package command.commands.economy;

import command.Command;
import command.util.cards.DeckOfCards;
import command.util.game.HiLoGame;
import database.Database;
import economy.EconomyService;
import economy.InsufficientFundsException;
import economy.WagerLimits;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code !hilo <stake>} starts, {@code !higher} / {@code !lower} guess,
 * {@code !cashout} stops after a correct guess.
 */
public class HiLo extends Command {

    public static final long IDLE_LIMIT_MS = 2 * 60 * 1000;
    static final Map<Long, HiLoGame> GAMES = new ConcurrentHashMap<>();

    public HiLo() {
        super("!hilo", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey());
    }

    @Override
    public void start(MessageReceivedEvent event) {
        long user = event.getAuthor().getIdLong();
        String[] args = event.getMessage().getContentRaw().split("\\s+");
        try {
            int stake = args.length == 2 ? Integer.parseInt(args[1]) : 0;
            if (stake < 1) {
                event.getChannel().sendMessage("Say `!hilo <stake>` with a stake of at least 1 GC. Guess `!higher` or `!lower` (ace high, a tie loses). One correct guess pays "
                        + "1x your stake and you may `!cashout`; two in a row pay 2x. A wrong guess forfeits the "
                        + "stake.").queue();
                return;
            }
            if (GAMES.containsKey(user)) {
                event.getChannel().sendMessage("You already have a round open.").queue();
                return;
            }
            String blocked = WagerLimits.check(user, stake);
            if (blocked != null) {
                event.getChannel().sendMessage(blocked).queue();
                return;
            }
            String reservation = "hilo:" + event.getMessageId();
            try {
                EconomyService.getInstance().reserve(user, stake, "hilo", event.getMessageId(), reservation);
            } catch (InsufficientFundsException e) {
                event.getChannel().sendMessage("You do not have " + stake + " GC spendable.").queue();
                return;
            }
            DeckOfCards deck = new DeckOfCards(1);
            deck.shuffle();
            HiLoGame game = new HiLoGame(user, stake, reservation, deck);
            if (GAMES.putIfAbsent(user, game) != null) {
                EconomyService.getInstance().refundReservation(reservation);
                return;
            }
            event.getChannel().sendMessage("Your card: " + game.getCurrent().toEmote() + "\nMaximum loss: " + stake
                    + " GC. Say `!higher` or `!lower`.")
                    .setActionRow(guessButtons(user, false)).queue();
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Say `!hilo <stake>`.").queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    /** Higher, Lower and (after a correct guess) Cash out. */
    static java.util.List<net.dv8tion.jda.api.interactions.components.ItemComponent> guessButtons(long user,
                                                                                                   boolean cashout) {
        java.util.List<net.dv8tion.jda.api.interactions.components.ItemComponent> out = new java.util.ArrayList<>();
        out.add(command.ui.Buttons.mine(user, "!higher", "Higher",
                net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.PRIMARY));
        out.add(command.ui.Buttons.mine(user, "!lower", "Lower",
                net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.PRIMARY));
        if (cashout) out.add(command.ui.Buttons.mine(user, "!cashout", "Cash out",
                net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SUCCESS));
        return out;
    }

    public static java.util.Collection<HiLoGame> openGames() {
        return new java.util.ArrayList<>(GAMES.values());
    }

    /** Settles a round and removes it; safe to call more than once. */
    public static int finish(HiLoGame game, int net) throws Exception {
        if (!GAMES.remove(game.getUserId(), game)) return 0;
        Database.inTransaction(c -> {
            EconomyService.settleReservationIn(c, game.getReservationId(), net, "hilo");
            return null;
        });
        return net;
    }

    /** {@code !higher}, {@code !lower}, {@code !cashout} share one handler. */
    public static class Move extends Command {
        private final String mode;

        public Move(String key) {
            super(key, true);
            this.mode = key.substring(1);
        }

        @Override
        public void start(MessageReceivedEvent event) {
            HiLoGame game = GAMES.get(event.getAuthor().getIdLong());
            if (game == null) {
                event.getChannel().sendMessage("No open round. Say `!hilo <stake>`.").queue();
                return;
            }
            try {
                synchronized (game) {
                    if (GAMES.get(game.getUserId()) != game) return;
                    if (mode.equals("cashout")) {
                        if (game.getCorrect() == 0) {
                            event.getChannel().sendMessage("Make a correct guess first.").queue();
                            return;
                        }
                        int net = finish(game, game.netIfStopped());
                        economy.Cosmetics.reply(event.getChannel(), game.getUserId(), "Cashed out: +" + net + " GC.", true);
                        return;
                    }
                    boolean ok = game.guess(mode.equals("higher"));
                    String card = "Next card: " + game.getCurrent().toEmote() + "\n";
                    if (!ok) {
                        finish(game, -game.getStake());
                        event.getChannel().sendMessage(card + "Wrong. You lost " + game.getStake() + " GC.").queue();
                    } else if (game.isFinished()) {
                        int net = finish(game, game.netIfStopped());
                        economy.Cosmetics.reply(event.getChannel(), game.getUserId(), card + "Two in a row! +" + net + " GC.", true);
                    } else {
                        event.getChannel().sendMessage(card + "Correct! Cash out for +" + game.netIfStopped()
                                + " GC with `!cashout`, or risk it all with `!higher` / `!lower` for +"
                                + game.getStake() * 2 + " GC.")
                                .setActionRow(guessButtons(game.getUserId(), true)).queue();
                    }
                }
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }
}

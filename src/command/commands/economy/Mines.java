package command.commands.economy;

import command.Command;
import command.ui.Buttons;
import command.util.art.GameArt;
import command.util.game.MinesGame;
import database.Database;
import economy.EconomyService;
import economy.Wagers;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Mines on a 4x4 board of buttons. {@code !mines <stake> [mines]} starts a round and holds the stake; press tiles
 * (or say {@code !mine <1-16>}) to uncover them and {@code !minecash} to take the payout. Hit a mine and the stake
 * is lost. Rounds nobody touches for five minutes are cashed out at their standing.
 */
public class Mines {

    public static final long IDLE_LIMIT_MS = 5 * 60 * 1000;

    private static final ConcurrentMap<Long, MinesGame> GAMES = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    private static final String USAGE = "Say `!mines <stake> [mines]` (1 to 8 mines, default 3). Press tiles to uncover "
            + "gems; each one raises your payout. Press **Cash out** to take it, hit a mine and you lose the stake.";

    private Mines() { }

    public static Collection<MinesGame> openGames() {
        return new ArrayList<>(GAMES.values());
    }

    /** Settles a round and removes it; safe to call more than once. */
    public static int finish(MinesGame game, int net) throws Exception {
        if (!GAMES.remove(game.getUserId(), game)) return 0;
        Database.inTransaction(c -> {
            EconomyService.settleReservationIn(c, game.getReservationId(), net, "mines");
            return null;
        });
        return net;
    }

    /** The 4x4 grid of tile buttons plus a Cash out button. Uncovered tiles show a gem and cannot be pressed. */
    static List<ActionRow> board(MinesGame game) {
        long user = game.getUserId();
        List<ActionRow> rows = new ArrayList<>();
        for (int r = 0; r < 4; r++) {
            List<ItemComponent> row = new ArrayList<>();
            for (int c = 0; c < 4; c++) {
                int tile = r * 4 + c;
                if (game.getFound().contains(tile)) {
                    row.add(Buttons.mine(user, "!mine " + (tile + 1), "💎", ButtonStyle.SUCCESS).asDisabled());
                } else {
                    row.add(Buttons.mine(user, "!mine " + (tile + 1), "?", ButtonStyle.SECONDARY));
                }
            }
            rows.add(ActionRow.of(row));
        }
        List<ItemComponent> cash = new ArrayList<>();
        Button cashOut = Buttons.mine(user, "!minecash", "Cash out +" + game.netIfStopped() + " GC", ButtonStyle.PRIMARY);
        cash.add(game.getFound().isEmpty() ? cashOut.asDisabled() : cashOut);
        rows.add(ActionRow.of(cash));
        return rows;
    }

    private static String status(MinesGame game) {
        return String.format("Mines: **%d** hidden. Gems found: **%d**. Payout now **x%.2f** (+%d GC). "
                        + "Next gem would pay x%.2f. Stake %d GC.",
                game.getMines().size(), game.getFound().size(), game.multiplier(), game.netIfStopped(),
                MinesGame.multiplier(game.getMines().size(), game.getFound().size() + 1), game.getStake());
    }

    /** {@code !mines <stake> [mines]}. */
    public static class Start extends Command {

        public Start() {
            super("!mines", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().startsWith(getKey());
        }

        @Override
        public void start(MessageReceivedEvent event) {
            long user = event.getAuthor().getIdLong();
            String[] args = event.getMessage().getContentRaw().trim().split("\\s+");
            int stake = 0;
            int mines = MinesGame.DEFAULT_MINES;
            try {
                if (args.length >= 2 && args.length <= 3) stake = Integer.parseInt(args[1]);
                if (args.length == 3) mines = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                stake = 0;
            }
            if (stake < 1 || mines < MinesGame.MIN_MINES || mines > MinesGame.MAX_MINES || args.length > 3) {
                event.getChannel().sendMessage(USAGE).queue();
                return;
            }
            try {
                if (GAMES.containsKey(user)) {
                    event.getChannel().sendMessage("You already have a mines round open.").queue();
                    return;
                }
                String blocked = Wagers.blocked(user, stake);
                if (blocked != null) {
                    event.getChannel().sendMessage(blocked).queue();
                    return;
                }
                String reservation = "mines:" + event.getMessageId();
                try {
                    EconomyService.getInstance().reserve(user, stake, "mines", event.getMessageId(), reservation);
                } catch (economy.InsufficientFundsException e) {
                    event.getChannel().sendMessage("You do not have " + stake + " GC spendable.").queue();
                    return;
                }
                MinesGame game = new MinesGame(user, stake, reservation, mines, RANDOM);
                if (GAMES.putIfAbsent(user, game) != null) {
                    EconomyService.getInstance().refundReservation(reservation);
                    return;
                }
                event.getChannel().sendMessage(status(game) + "\nMaximum loss: " + stake + " GC.")
                        .setComponents(board(game)).queue();
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }

    /** {@code !mine <1-16>} uncovers a tile; {@code !minecash} takes the payout. */
    public static class Move extends Command {

        private final boolean cash;

        public Move(String key) {
            super(key, true);
            this.cash = key.equals("!minecash");
        }

        @Override
        public boolean keyMatches(String string) {
            String lower = string.toLowerCase();
            return cash ? lower.equals("!minecash") : lower.startsWith("!mine ") || lower.equals("!mine");
        }

        @Override
        public void start(MessageReceivedEvent event) {
            long user = event.getAuthor().getIdLong();
            MessageChannel channel = event.getChannel();
            MinesGame game = GAMES.get(user);
            if (game == null) {
                channel.sendMessage("No open mines round. Say `!mines <stake>`.").queue();
                return;
            }
            try {
                synchronized (game) {
                    if (GAMES.get(user) != game) return;
                    if (cash) {
                        if (game.getFound().isEmpty()) {
                            channel.sendMessage("Uncover a tile first.").queue();
                            return;
                        }
                        end(channel, game, game.netIfStopped(), -1, "CASHED OUT  x" + String.format("%.2f", game.multiplier()), true);
                        return;
                    }
                    String[] args = event.getMessage().getContentRaw().trim().split("\\s+");
                    int tile;
                    try {
                        tile = Integer.parseInt(args[1]) - 1;
                    } catch (RuntimeException e) {
                        tile = -1;
                    }
                    if (tile < 0 || tile >= MinesGame.TILES) {
                        channel.sendMessage("Say `!mine <1-16>`.").queue();
                        return;
                    }
                    MinesGame.Result result = game.pick(tile);
                    if (result == MinesGame.Result.ALREADY) {
                        channel.sendMessage("That tile is already open.").queue();
                    } else if (result == MinesGame.Result.MINE) {
                        end(channel, game, -game.getStake(), tile, "BOOM  -" + game.getStake() + " GC", false);
                    } else if (game.allSafeFound()) {
                        end(channel, game, game.netIfStopped(), -1, "CLEARED  x" + String.format("%.2f", game.multiplier()), true);
                    } else {
                        channel.sendMessage("💎 Safe! " + status(game)).setComponents(board(game)).queue();
                    }
                }
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }

        private static void end(MessageChannel channel, MinesGame game, int net, int exploded, String label, boolean won)
                throws Exception {
            finish(game, net);
            String text = (won ? economy.Cosmetics.winHeader(game.getUserId()) : "")
                    + (won ? "You cashed out for +" + net + " GC!" : "You hit a mine and lost " + game.getStake() + " GC.");
            channel.sendFiles(FileUpload.fromData(GameArt.minesBoard(game.getFound(), game.getMines(), exploded, label, won),
                            "mines.png"))
                    .setContent(text)
                    .setActionRow(Buttons.mine(game.getUserId(), "!mines " + game.getStake() + " " + game.getMines().size(),
                            "Play again (" + game.getStake() + " GC)", ButtonStyle.PRIMARY)).queue();
        }
    }
}

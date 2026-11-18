package command.commands.misc;

import command.Command;
import command.ui.Buttons;
import command.ui.Reveal;
import command.util.art.GameArt;
import database.Database;
import database.connectors.EconomyConnector;
import economy.EconomyService;
import economy.WagerLimits;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * {@code !flip} flips a coin for fun. {@code !flip <heads|tails> <stake>} bets on it: call it right and win your
 * stake, call it wrong and lose it. The coin is an animation that spins and lands on the result.
 */
public class Flip extends Command {

    private final Random random = new Random();
    private final EconomyConnector ec = new EconomyConnector();

    /**
     * Initializes the command's key to "!flip".
     */
    public Flip() {
        super("!flip", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey());
    }

    @Override
    public void start(MessageReceivedEvent event) {
        long user = event.getAuthor().getIdLong();
        String[] args = event.getMessage().getContentRaw().trim().split("\\s+");
        Boolean call = args.length >= 2 ? parseSide(args[1]) : null;
        int stake = 0;
        try {
            if (args.length > 3 || (args.length >= 2 && call == null)) {
                event.getChannel().sendMessage("Say `!flip` for a plain flip, or `!flip <heads|tails> <stake>` to bet.")
                        .queue();
                return;
            }
            if (args.length == 3) {
                stake = Integer.parseInt(args[2]);
                if (stake < 1) {
                    event.getChannel().sendMessage("The stake must be at least 1 GC.").queue();
                    return;
                }
            } else if (call != null) {
                event.getChannel().sendMessage("Add a stake to bet: `!flip " + args[1] + " <stake>`.").queue();
                return;
            }
            if (stake > 0) {
                String blocked = WagerLimits.check(user, stake);
                if (blocked != null) {
                    event.getChannel().sendMessage(blocked).queue();
                    return;
                }
                if (stake > ec.getSpendable(user)) {
                    event.getChannel().sendMessage("You do not have " + stake + " GC spendable.").queue();
                    return;
                }
            }

            boolean heads = random.nextBoolean();
            boolean won = call != null && call == heads;
            if (stake > 0) {
                try {
                    settle(user, stake, won ? stake : -stake, event.getMessageId());
                } catch (EconomyService.InsufficientFundsRuntime e) {
                    event.getChannel().sendMessage("You do not have " + stake + " GC spendable.").queue();
                    return;
                }
            }

            String name = economy.Cosmetics.nameWithTitle(user, event.getAuthor().getName());
            String face = heads ? "Heads" : "Tails";
            String text;
            if (stake == 0) {
                text = face + "!";
            } else if (won) {
                text = economy.Cosmetics.winHeader(user) + name + " called " + side(call) + ": **" + face + "**. You won "
                        + stake + " GC!";
            } else {
                text = name + " called " + side(call) + ": **" + face + "**. You lost " + stake + " GC.";
            }
            if (stake > 0) text += " Maximum loss was " + stake + " GC.";

            List<ItemComponent> buttons = new ArrayList<>();
            if (stake > 0) {
                buttons.add(Buttons.mine(user, "!flip " + side(call).toLowerCase() + " " + stake,
                        "Again (" + stake + " GC)", ButtonStyle.PRIMARY));
            } else {
                buttons.add(Buttons.mine(user, "!flip", "Flip again", ButtonStyle.PRIMARY));
            }
            Reveal.send(event.getChannel().sendFiles(FileUpload.fromData(GameArt.coinSpin(heads), "coin.gif"))
                    .setContent("Flipping…"), event.getChannel(), text, buttons, 1500);
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("The stake must be a whole number: `!flip <heads|tails> <stake>`.").queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    private static Boolean parseSide(String s) {
        switch (s.toLowerCase()) {
            case "h":
            case "heads":
                return Boolean.TRUE;
            case "t":
            case "tails":
                return Boolean.FALSE;
            default:
                return null;
        }
    }

    private static String side(Boolean heads) {
        return heads ? "Heads" : "Tails";
    }

    /** Reserve and settle in one transaction, so a crash cannot leave the stake held. */
    private static void settle(long user, int stake, int net, String messageId) throws Exception {
        String reservation = "flip:" + messageId;
        Database.inTransaction(c -> {
            EconomyService.reserveIn(c, user, stake, "flip", messageId, reservation);
            EconomyService.settleReservationIn(c, reservation, net, "flip");
            return null;
        });
    }
}

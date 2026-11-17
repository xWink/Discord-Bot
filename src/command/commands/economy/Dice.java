package command.commands.economy;

import command.Command;
import command.commands.blackjack.Bet;
import command.util.game.DiceGame;
import database.Database;
import economy.EconomyService;
import economy.WagerLimits;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Random;

/**
 * {@code !dice <low|high|seven> [stake]}. Without a stake it is free practice.
 */
public class Dice extends Command {

    private static final String ODDS = "Low (2-6) and high (8-12) win 15/36 and pay 1:1. Seven wins 6/36 and pays 4:1.";

    private final Random random = new Random();

    public Dice() {
        super("!dice", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey());
    }

    @Override
    public void start(MessageReceivedEvent event) {
        String[] args = event.getMessage().getContentRaw().split("\\s+");
        DiceGame.Pick pick = args.length >= 2 ? DiceGame.Pick.parse(args[1]) : null;
        if (pick == null || args.length > 3) {
            event.getChannel().sendMessage("Say `!dice <low|high|seven> [stake]`. " + ODDS
                    + " Leave the stake off to play free.").queue();
            return;
        }

        int stake = 0;
        try {
            if (args.length == 3) {
                stake = Integer.parseInt(args[2]);
                if (stake < 1 || stake > Bet.MAX_STAKE) {
                    event.getChannel().sendMessage("Stakes run from 1 to " + Bet.MAX_STAKE + " GC.").queue();
                    return;
                }
                String blocked = WagerLimits.check(event.getAuthor().getIdLong(), stake);
                if (blocked != null) {
                    event.getChannel().sendMessage(blocked).queue();
                    return;
                }
            }

            int[] dice = DiceGame.roll(random);
            int total = dice[0] + dice[1];
            int net = stake > 0 ? DiceGame.net(pick, total, stake) : 0;
            String name = economy.Cosmetics.nameWithTitle(event.getAuthor().getIdLong(), event.getAuthor().getName());
            String roll = ":game_die: " + dice[0] + " + " + dice[1] + " = **" + total + "**";

            if (stake > 0) {
                try {
                    settle(event.getAuthor().getIdLong(), stake, net, event.getMessageId());
                } catch (EconomyService.InsufficientFundsRuntime e) {
                    event.getChannel().sendMessage("You do not have " + stake + " GC spendable.").queue();
                    return;
                }
            }

            boolean won = DiceGame.wins(pick, total);
            String result = stake == 0
                    ? (won ? "You called it (free play)." : "Missed (free play).")
                    : (won ? "You won " + net + " GC!" : "You lost " + stake + " GC.");
            economy.Cosmetics.reply(event.getChannel(), event.getAuthor().getIdLong(), name + " picked "
                    + pick.name().toLowerCase() + ". " + roll + "\n" + result
                    + (stake > 0 ? " Maximum loss was " + stake + " GC." : ""), won && stake > 0);
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    /** Reserve and settle in one transaction, so a crash cannot leave the stake held. */
    private static void settle(long user, int stake, int net, String messageId) throws Exception {
        String reservation = "dice:" + messageId;
        Database.inTransaction(c -> {
            EconomyService.reserveIn(c, user, stake, "dice", messageId, reservation);
            EconomyService.settleReservationIn(c, reservation, net, "dice");
            return null;
        });
    }
}

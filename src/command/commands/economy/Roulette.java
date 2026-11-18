package command.commands.economy;

import command.Command;
import command.ui.Buttons;
import command.ui.Reveal;
import command.util.art.GameArt;
import command.util.game.RouletteGame;
import economy.EconomyService;
import economy.Wagers;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.Random;

/**
 * {@code !roulette <bet> <stake>}. Bet on red, black, odd, even, low (1-18), high (19-36) for 1:1, or on a single
 * number (0-36) for 35:1. One zero, so the house edge is 2.7%.
 */
public class Roulette extends Command {

    private static final String USAGE = "Say `!roulette <bet> <stake>`. Bets: `red`, `black`, `odd`, `even`, `low` (1-18), "
            + "`high` (19-36) pay 1:1; a number from 0 to 36 pays 35:1. Zero loses every bet but a bet on zero.";

    private final Random random = new Random();

    public Roulette() {
        super("!roulette", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey());
    }

    @Override
    public void start(MessageReceivedEvent event) {
        long user = event.getAuthor().getIdLong();
        String[] args = event.getMessage().getContentRaw().trim().split("\\s+");
        RouletteGame.Bet bet = args.length == 3 ? RouletteGame.parse(args[1]) : null;
        int stake = 0;
        try {
            if (bet != null) stake = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            bet = null;
        }
        if (bet == null || stake < 1) {
            event.getChannel().sendMessage(USAGE).queue();
            return;
        }
        try {
            String blocked = Wagers.gate(user, stake);
            if (blocked != null) {
                event.getChannel().sendMessage(blocked).queue();
                return;
            }
            int result = RouletteGame.spin(random);
            int net = RouletteGame.net(bet, result, stake);
            try {
                Wagers.settleNow(user, stake, net, event.getMessageId(), "roulette");
            } catch (EconomyService.InsufficientFundsRuntime e) {
                event.getChannel().sendMessage("You do not have " + stake + " GC spendable.").queue();
                return;
            }
            String colour = result == 0 ? "green" : RouletteGame.isRed(result) ? "red" : "black";
            String name = economy.Cosmetics.nameWithTitle(user, event.getAuthor().getName());
            String text = (net > 0 ? economy.Cosmetics.winHeader(user) : "") + name + " bet " + bet.describe() + ". The ball "
                    + "landed on **" + result + " " + colour + "**. " + (net > 0 ? "You won " + net + " GC!"
                    : "You lost " + stake + " GC.") + " Maximum loss was " + stake + " GC.";
            Reveal.send(event.getChannel().sendFiles(FileUpload.fromData(GameArt.roulette(result), "roulette.gif"))
                            .setContent("The wheel is spinning…"), event.getChannel(), text,
                    Buttons.single(Buttons.mine(user, "!roulette " + args[1].toLowerCase() + " " + stake,
                            "Spin again (" + stake + " GC)", ButtonStyle.PRIMARY)), 2000);
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

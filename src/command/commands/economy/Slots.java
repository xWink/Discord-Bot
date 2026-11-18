package command.commands.economy;

import command.Command;
import command.util.art.GameArt;
import command.util.game.SlotsGame;
import database.connectors.EconomyConnector;
import economy.EconomyService;
import economy.WagerLimits;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.Random;

/** {@code !slots <stake>}. Three reels spin as an animation; the result is settled before it is shown. */
public class Slots extends Command {

    private static final String PAYTABLE = "Three alike pay: cherry x3, lemon x5, bell x8, star x15, seven x25, "
            + "mascot x50. Any two alike pay x1.4.";

    private final Random random = new Random();
    private final EconomyConnector ec = new EconomyConnector();

    public Slots() {
        super("!slots", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey());
    }

    @Override
    public void start(MessageReceivedEvent event) {
        long user = event.getAuthor().getIdLong();
        String[] args = event.getMessage().getContentRaw().split("\\s+");
        int stake;
        try {
            stake = args.length == 2 ? Integer.parseInt(args[1]) : 0;
        } catch (NumberFormatException e) {
            stake = 0;
        }
        if (stake < 1) {
            event.getChannel().sendMessage("Say `!slots <stake>`. " + PAYTABLE).queue();
            return;
        }
        try {
            String blocked = WagerLimits.check(user, stake);
            if (blocked != null) {
                event.getChannel().sendMessage(blocked).queue();
                return;
            }
            int spendable = ec.getSpendable(user);
            if (stake > spendable) {
                event.getChannel().sendMessage("You do not have " + stake + " GC spendable.").queue();
                return;
            }

            SlotsGame.Symbol[] reels = SlotsGame.spin(random);
            int net = SlotsGame.net(reels, stake);
            try {
                economy.Wagers.settleNow(user, stake, net, event.getMessageId(), "slots");
            } catch (EconomyService.InsufficientFundsRuntime e) {
                event.getChannel().sendMessage("You do not have " + stake + " GC spendable.").queue();
                return;
            }

            boolean won = net > 0;
            String label = SlotsGame.label(reels);
            String name = economy.Cosmetics.nameWithTitle(user, event.getAuthor().getName());
            String result = net > 0 ? "You won " + net + " GC!" : net == 0 ? "A push: your stake came back."
                    : "You lost " + stake + " GC.";

            String text = (won ? economy.Cosmetics.winHeader(user) : "") + name + ": **" + label + "**\n" + result
                    + " Stake " + stake + " GC, maximum loss was " + stake + " GC.";
            command.ui.Reveal.send(event.getChannel()
                            .sendFiles(FileUpload.fromData(GameArt.slots(reels, label, won), "slots.gif"))
                            .setContent("Spinning\u2026"),
                    event.getChannel(), text,
                    command.ui.Buttons.single(command.ui.Buttons.mine(user, "!slots " + stake,
                            "Spin again (" + stake + " GC)", ButtonStyle.PRIMARY)), 2200);
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

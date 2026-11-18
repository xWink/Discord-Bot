package command.commands.economy;

import command.Command;
import command.ui.Buttons;
import command.ui.Reveal;
import command.util.art.GameArt;
import command.util.game.CrashGame;
import economy.EconomyService;
import economy.Wagers;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.Random;

/**
 * {@code !crash <stake> [target]}. A multiplier climbs and crashes at a random point. Name the multiplier you want
 * to cash out at (default 2x): reach it and you win your stake times (target - 1), crash first and you lose the stake.
 * Higher targets pay more and happen less often; every target returns about 97%.
 */
public class Crash extends Command {

    private static final String USAGE = "Say `!crash <stake> [target]`. The multiplier climbs until it crashes. Pick the "
            + "multiplier to cash out at (1.1 to 1000, default 2): reach it and you win your stake times (target - 1), "
            + "crash first and you lose the stake. Chance to win is about 97 divided by the target, in percent.";

    private final Random random = new Random();

    public Crash() {
        super("!crash", true);
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
        int target = CrashGame.DEFAULT_TARGET;
        try {
            if (args.length >= 2 && args.length <= 3) stake = Integer.parseInt(args[1]);
            if (args.length == 3) target = CrashGame.parseTarget(args[2]);
        } catch (NumberFormatException e) {
            stake = 0;
        }
        if (stake < 1 || target < 0 || args.length > 3) {
            event.getChannel().sendMessage(USAGE).queue();
            return;
        }
        try {
            String blocked = Wagers.gate(user, stake);
            if (blocked != null) {
                event.getChannel().sendMessage(blocked).queue();
                return;
            }
            int crashPoint = CrashGame.crashPoint(random);
            int net = CrashGame.net(crashPoint, target, stake);
            boolean won = CrashGame.wins(crashPoint, target);
            try {
                Wagers.settleNow(user, stake, net, event.getMessageId(), "crash");
            } catch (EconomyService.InsufficientFundsRuntime e) {
                event.getChannel().sendMessage("You do not have " + stake + " GC spendable.").queue();
                return;
            }
            String name = economy.Cosmetics.nameWithTitle(user, event.getAuthor().getName());
            String text = (won && net > 0 ? economy.Cosmetics.winHeader(user) : "") + name + " aimed for "
                    + CrashGame.show(target) + ". It crashed at **" + CrashGame.show(crashPoint) + "**. "
                    + (won ? "You cashed out and won " + net + " GC!" : "You lost " + stake + " GC.")
                    + " Maximum loss was " + stake + " GC.";
            Reveal.send(event.getChannel().sendFiles(FileUpload.fromData(GameArt.crash(crashPoint, target, won), "crash.gif"))
                            .setContent("The multiplier is climbing…"), event.getChannel(), text,
                    Buttons.single(Buttons.mine(user, "!crash " + stake + " " + target / 100.0,
                            "Again (" + stake + " GC at " + CrashGame.show(target) + ")", ButtonStyle.PRIMARY)), 2200);
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

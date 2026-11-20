package command.commands.economy;

import command.Command;
import command.util.game.DiceGame;
import economy.EconomyService;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

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
                if (stake < 1) {
                    event.getChannel().sendMessage("The stake must be at least 1 GC.").queue();
                    return;
                }
            }
            String blocked = stake > 0 ? economy.Wagers.gate(event.getAuthor().getIdLong(), stake)
                    : economy.Wagers.cooldown(event.getAuthor().getIdLong());
            if (blocked != null) {
                event.getChannel().sendMessage(blocked).queue();
                return;
            }

            int[] dice = DiceGame.roll(random);
            int total = dice[0] + dice[1];
            int net = stake > 0 ? DiceGame.net(pick, total, stake) : 0;
            String name = economy.Cosmetics.nameWithTitle(event.getAuthor().getIdLong(), event.getAuthor().getName());

            if (stake > 0) {
                try {
                    economy.Wagers.settleNow(event.getAuthor().getIdLong(), stake, net, event.getMessageId(), "dice");
                } catch (EconomyService.InsufficientFundsRuntime e) {
                    event.getChannel().sendMessage("You do not have " + stake + " GC spendable.").queue();
                    return;
                }
            }

            boolean won = DiceGame.wins(pick, total);
            String result = stake == 0
                    ? (won ? "You called it (free play)." : "Missed (free play).")
                    : (won ? "You won " + net + " GC!" : "You lost " + stake + " GC.");
            String text = (won && stake > 0 ? economy.Cosmetics.winHeader(event.getAuthor().getIdLong()) : "") + name
                    + " picked " + pick.name().toLowerCase() + ", rolled **" + total + "**. " + result
                    + (stake > 0 ? " Maximum loss was " + stake + " GC." : "");
            long user = event.getAuthor().getIdLong();
            java.util.List<ItemComponent> again = command.ui.Buttons.single(
                    command.ui.Buttons.mine(user, "!dice " + pick.name().toLowerCase() + (stake > 0 ? " " + stake : ""),
                            "Roll again" + (stake > 0 ? " (" + stake + " GC)" : ""),
                            ButtonStyle.PRIMARY));
            command.ui.Reveal.render(event.getChannel(), "dice.gif", "Rolling\u2026",
                    () -> command.util.art.GameArt.diceRoll(dice[0], dice[1]), text, again, 1400);
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

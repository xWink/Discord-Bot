package command.commands.economy;

import command.Command;
import command.ui.Buttons;
import command.ui.Reveal;
import command.util.art.GameArt;
import command.util.game.ScratchGame;
import economy.EconomyService;
import economy.Wagers;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.Random;

/** {@code !scratch <stake>}. Nine cells, scratched off one by one; match three to win that symbol's prize. */
public class Scratch extends Command {

    private static final String USAGE = "Say `!scratch <stake>`. Match three symbols to win: cherry x1.5, lemon x3, "
            + "bell x5, star x10, mascot x50.";

    private final Random random = new Random();

    public Scratch() {
        super("!scratch", true);
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
        try {
            if (args.length == 2) stake = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            stake = 0;
        }
        if (stake < 1) {
            event.getChannel().sendMessage(USAGE).queue();
            return;
        }
        try {
            String blocked = Wagers.blocked(user, stake);
            if (blocked != null) {
                event.getChannel().sendMessage(blocked).queue();
                return;
            }
            ScratchGame.Card card = ScratchGame.deal(random);
            int net = ScratchGame.net(card, stake);
            try {
                Wagers.settleNow(user, stake, net, event.getMessageId(), "scratch");
            } catch (EconomyService.InsufficientFundsRuntime e) {
                event.getChannel().sendMessage("You do not have " + stake + " GC spendable.").queue();
                return;
            }
            boolean won = card.prize != null;
            String label = won ? "THREE " + card.prize.symbol.name() + "S  x" + card.prize.tenths / 10.0 : "NO MATCH";
            String name = economy.Cosmetics.nameWithTitle(user, event.getAuthor().getName());
            String text = (net > 0 ? economy.Cosmetics.winHeader(user) : "") + name + ": **" + label + "**\n"
                    + (net > 0 ? "You won " + net + " GC!" : net == 0 ? "A push: your stake came back."
                    : "You lost " + stake + " GC.") + " Maximum loss was " + stake + " GC.";
            Reveal.send(event.getChannel().sendFiles(FileUpload.fromData(GameArt.scratch(card, label, won), "scratch.gif"))
                            .setContent("Scratching…"), event.getChannel(), text,
                    Buttons.single(Buttons.mine(user, "!scratch " + stake, "New card (" + stake + " GC)",
                            ButtonStyle.PRIMARY)), 2700);
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

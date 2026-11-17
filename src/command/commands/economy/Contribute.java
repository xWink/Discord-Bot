package command.commands.economy;

import command.Command;
import economy.GoalService;
import economy.InsufficientFundsException;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Contribute extends Command {

    public Contribute() {
        super("!contribute", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey());
    }

    /**
     * {@code !contribute 10} previews the amount and warns it is final;
     * {@code !contribute 10 confirm} charges it.
     */
    @Override
    public void start(MessageReceivedEvent event) {
        String[] args = event.getMessage().getContentRaw().split("\\s+");
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            event.getChannel().sendMessage("Say `!contribute <amount>`, for example `!contribute 10`.").queue();
            return;
        }

        try {
            GoalService.Goal goal = GoalService.getInstance().current();
            if (goal == null || !goal.status.equals(GoalService.ACTIVE)) {
                event.getChannel().sendMessage("There is no open community goal right now.").queue();
                return;
            }
            int charge = Math.min(amount, goal.target - goal.raised);
            boolean confirmed = args.length >= 3 && args[2].equalsIgnoreCase("confirm");
            if (!confirmed) {
                event.getChannel().sendMessage("This will charge **" + charge + " GC** toward \"" + goal.title
                        + "\". Contributions are final and cannot be refunded.\nSay `!contribute " + amount
                        + " confirm` to go ahead.").queue();
                return;
            }
            try {
                GoalService.Outcome out = GoalService.getInstance().contribute(event.getAuthor().getIdLong(), amount,
                        "goal:" + event.getMessageId());
                if (out == null || out.charged == 0) {
                    event.getChannel().sendMessage("The goal is already full.").queue();
                    return;
                }
                event.getChannel().sendMessage(GoalCommand.embed(out.goal).build()).queue();
                if (out.reachedNow) {
                    event.getChannel().sendMessage("**Goal reached!** \"" + out.goal.title
                            + "\" is unlocked. Staff will schedule it and announce the time.").queue();
                }
            } catch (InsufficientFundsException e) {
                event.getChannel().sendMessage("You only have " + e.getSpendable() + " spendable GC.").queue();
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

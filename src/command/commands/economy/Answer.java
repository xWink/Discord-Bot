package command.commands.economy;

import arcade.ArcadeService;
import arcade.PunchCard;
import command.Command;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Answer extends Command {

    public Answer() {
        super("!answer", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey());
    }

    @Override
    public void start(MessageReceivedEvent event) {
        String[] args = event.getMessage().getContentRaw().split("\\s+");
        if (args.length != 2 || args[1].length() != 1 || !Character.isLetter(args[1].charAt(0))) {
            event.getChannel().sendMessage("Say `!answer <letter>`, for example `!answer b`.").queue();
            return;
        }
        int choice = Character.toUpperCase(args[1].charAt(0)) - 'A';
        try {
            ArcadeService.Result r = ArcadeService.answer(event.getAuthor().getIdLong(), choice);
            if (r == null) {
                event.getChannel().sendMessage("You have no open round. Say `!arcade` to start one.").queue();
                return;
            }
            StringBuilder out = new StringBuilder();
            String name = event.getAuthor().getName();
            String answer = r.question.getChoices().get(r.question.getCorrectIndex());
            if (r.timedOut) {
                out.append("Time's up, ").append(name).append(". The answer was **").append(answer).append("**.");
            } else {
                out.append(r.correct ? "Correct, " : "Not quite, ").append(name).append(". The answer was **")
                        .append(answer).append("**.");
                if (r.question.getExplanation() != null) out.append('\n').append(r.question.getExplanation());
                if (r.dailyPaid) out.append("\nDaily reward: +5 GC");
                else out.append("\nDaily reward already claimed. This round was free practice.");
                if (r.punchPaid) out.append("\nPunch card complete: +").append(PunchCard.REWARD).append(" GC!");
            }
            out.append("\n_Source: ").append(r.question.getAttribution()).append(" (")
                    .append(r.question.getLicense()).append(")_");
            event.getChannel().sendMessage(out.toString()).queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

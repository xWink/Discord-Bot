package command.commands.economy;

import arcade.ArcadeService;
import arcade.PunchCard;
import arcade.Question;
import command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
import java.util.List;

/**
 * {@code !arcade} starts today's featured free round; {@code !answer A-D}
 * submits it.
 */
public class Arcade extends Command {

    private static final String[] LETTERS = {"A", "B", "C", "D", "E", "F"};

    public Arcade() {
        super("!arcade", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.equalsIgnoreCase(getKey());
    }

    @Override
    public void start(MessageReceivedEvent event) {
        try {
            ArcadeService.Round round = ArcadeService.start(event.getAuthor().getIdLong());
            if (round == null) {
                event.getChannel().sendMessage("The arcade has no questions loaded yet. Try again soon.").queue();
                return;
            }
            Question q = round.question;
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.YELLOW);
            eb.setTitle("Daily arcade: " + (q.getKind().equals("code") ? "Code reading" : "Trivia"));
            eb.setDescription(q.getPrompt());
            eb.addField("Category", q.getCategory() + " · " + q.getDifficulty(), true);
            eb.addField("Time", ArcadeService.ROUND_SECONDS + " seconds", true);
            eb.addField("Entry", "Free", true);
            eb.addField("Choices", choices(q.getChoices()), false);
            eb.setFooter("Answer with !answer <letter>. Your first round each day pays the daily reward. "
                    + "Punch card: " + PunchCard.daysThisWeek(event.getAuthor().getIdLong()) + "/"
                    + PunchCard.DAYS_NEEDED + " days this week.", null);
            event.getChannel().sendMessageEmbeds(eb.build())
                    .setActionRow(command.ui.Buttons.answers(event.getAuthor().getIdLong(), true, "!answer",
                            q.getChoices().size())).queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    static String choices(List<String> choices) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < choices.size() && i < LETTERS.length; i++) {
            sb.append("**").append(LETTERS[i]).append("**  ").append(choices.get(i)).append('\n');
        }
        return sb.toString();
    }
}

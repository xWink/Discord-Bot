package command.commands.economy;

import arcade.ArcadeService;
import arcade.PersonalTrivia;
import arcade.Question;
import arcade.QuestionBank;
import command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
import java.util.Arrays;

/**
 * {@code !ptrivia <category> [cost]} starts a personal round; anyone in the
 * channel answers with {@code !panswer <letter>} within a minute.
 */
public final class PersonalTriviaCommands {

    private PersonalTriviaCommands() { }

    public static class Start extends Command {
        public Start() { super("!ptrivia", true); }

        @Override
        public boolean keyMatches(String s) { return s.toLowerCase().startsWith("!ptrivia"); }

        @Override
        public void start(MessageReceivedEvent event) {
            String[] a = event.getMessage().getContentRaw().split("\\s+");
            try {
                if (a.length < 2) {
                    event.getChannel().sendMessage("Pick a category and pay " + PersonalTrivia.MIN_COST + "-"
                            + PersonalTrivia.MAX_COST + " GC: `!ptrivia <category> [cost]`. Friends join free with "
                            + "`!panswer`.\nCategories: " + String.join(", ", QuestionBank.categories("trivia")))
                            .queue();
                    return;
                }
                int cost = PersonalTrivia.MIN_COST;
                int end = a.length;
                try {
                    cost = Integer.parseInt(a[a.length - 1]);
                    end = a.length - 1;
                } catch (NumberFormatException ignored) { }
                String category = String.join(" ", Arrays.copyOfRange(a, 1, end));
                Object r = PersonalTrivia.start(event.getAuthor().getIdLong(), event.getChannel().getIdLong(),
                        category, cost, "ptrivia:" + event.getMessageId());
                if (r instanceof String) {
                    event.getChannel().sendMessage((String) r).queue();
                    return;
                }
                Question q = ((PersonalTrivia.Round) r).question;
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Color.YELLOW);
                eb.setTitle(event.getAuthor().getName() + "'s trivia round · " + q.getCategory());
                eb.setDescription(q.getPrompt());
                eb.addField("Choices", Arcade.choices(q.getChoices()), false);
                eb.addField("Join", "Free for everyone: `!panswer <letter>` within 60 seconds", false);
                eb.setFooter("Host paid " + cost + " GC. Answers count toward your daily and punch card as usual.", null);
                event.getChannel().sendMessageEmbeds(eb.build()).queue();
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }

    public static class Answer extends Command {
        public Answer() { super("!panswer", true); }

        @Override
        public boolean keyMatches(String s) { return s.toLowerCase().startsWith("!panswer"); }

        @Override
        public void start(MessageReceivedEvent event) {
            String[] a = event.getMessage().getContentRaw().split("\\s+");
            if (a.length != 2 || a[1].length() != 1 || !Character.isLetter(a[1].charAt(0))) return;
            try {
                PersonalTrivia.Round round = PersonalTrivia.current(event.getChannel().getIdLong());
                ArcadeService.Result r = PersonalTrivia.answer(event.getChannel().getIdLong(),
                        event.getAuthor().getIdLong(), Character.toUpperCase(a[1].charAt(0)) - 'A');
                if (r == null || round == null) return;
                economy.Cosmetics.reply(event.getChannel(), event.getAuthor().getIdLong(),
                        economy.Cosmetics.nameWithTitle(event.getAuthor().getIdLong(), event.getAuthor().getName())
                        + (r.correct ? " got it!" : " missed.")
                        + (r.dailyPaid ? " Daily reward +5 GC." : "")
                        + (r.punchPaid ? " Punch card +" + arcade.PunchCard.REWARD + " GC!" : ""), r.correct);
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }
}

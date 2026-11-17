package command.commands.economy;

import arcade.Question;
import arcade.QuizService;
import command.Command;
import economy.WeeklyChallenge;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;

/**
 * Three-question runs: {@code !weekly <code|trivia>} and
 * {@code !gauntlet [prize]}. Answers use {@code !answer}.
 */
public final class QuizCommands {

    private QuizCommands() { }

    static void ask(MessageChannel channel, QuizService.Session s) {
        Question q = s.current();
        String title = (s.mode == QuizService.Mode.WEEKLY ? "Weekly challenge" : "Puzzle gauntlet"
                + (s.mode == QuizService.Mode.PRIZE ? " (prize run)" : " (practice)"))
                + " · question " + s.number() + "/" + QuizService.QUESTIONS;
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.YELLOW);
        eb.setTitle(title);
        eb.setDescription(q.getPrompt());
        eb.addField("Category", q.getCategory() + " · " + q.getDifficulty(), true);
        eb.addField("Time", arcade.ArcadeService.ROUND_SECONDS + " seconds", true);
        eb.addField("Choices", Arcade.choices(q.getChoices()), false);
        eb.setFooter("Answer with !answer <letter>", null);
        channel.sendMessageEmbeds(eb.build()).queue();
    }

    /** Called by {@code !answer} when the player has an open run. */
    static void report(MessageReceivedEvent event, QuizService.Step st) {
        StringBuilder out = new StringBuilder();
        String answer = st.answered.getChoices().get(st.answered.getCorrectIndex());
        out.append(st.timedOut ? "Too slow. " : st.correct ? "Correct. " : "Not quite. ")
                .append("The answer was **").append(answer).append("**.");
        if (st.correct) out.append(economy.Cosmetics.extras(event.getAuthor().getIdLong()).effect);
        if (st.answered.getExplanation() != null) out.append('\n').append(st.answered.getExplanation());
        if (st.next != null) {
            event.getChannel().sendMessage(out.toString()).queue();
            ask(event.getChannel(), st.next);
            return;
        }
        if (st.correctTotal == QuizService.QUESTIONS) out.append(economy.Cosmetics.extras(event.getAuthor().getIdLong()).effect);
        out.append("\n**Run complete: ").append(st.correctTotal).append("/").append(QuizService.QUESTIONS)
                .append(".**");
        if (st.reward > 0) out.append(" +").append(st.reward).append(" GC!");
        if (st.feeTaken) out.append(" The ").append(QuizService.ENTRY_FEE).append(" GC entry fee was spent.");
        if (st.prizeAwarded) out.append("\nYou earned this week's gauntlet badge (see `!shop owned`).");
        event.getChannel().sendMessage(out.toString()).queue();
    }

    public static class Weekly extends Command {
        public Weekly() { super("!weekly", true); }

        @Override
        public boolean keyMatches(String s) { return s.toLowerCase().startsWith("!weekly"); }

        @Override
        public void start(MessageReceivedEvent event) {
            String[] args = event.getMessage().getContentRaw().split("\\s+");
            String kind = args.length >= 2 ? args[1].toLowerCase() : "";
            if (!kind.equals("code") && !kind.equals("trivia")) {
                event.getChannel().sendMessage("**Weekly challenge** (pays once a week): get 2 of 3 right for "
                        + WeeklyChallenge.BASE_REWARD + " GC, all 3 for " + WeeklyChallenge.PERFECT_REWARD
                        + " GC.\nPick one: `!weekly code` (code reading), `!weekly trivia`, or take part in a hosted "
                        + "event. You get " + WeeklyChallenge.MAX_ATTEMPTS + " attempts a week. Free to play.")
                        .queue();
                return;
            }
            try {
                Object r = QuizService.start(event.getAuthor().getIdLong(), QuizService.Mode.WEEKLY, kind,
                        event.getChannel().getIdLong(), event.getMessageId());
                if (r instanceof String) event.getChannel().sendMessage((String) r).queue();
                else ask(event.getChannel(), (QuizService.Session) r);
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }

    public static class Gauntlet extends Command {
        public Gauntlet() { super("!gauntlet", true); }

        @Override
        public boolean keyMatches(String s) { return s.toLowerCase().startsWith("!gauntlet"); }

        @Override
        public void start(MessageReceivedEvent event) {
            String[] args = event.getMessage().getContentRaw().split("\\s+");
            boolean prize = args.length >= 2 && args[1].equalsIgnoreCase("prize");
            if (args.length >= 2 && !prize) {
                event.getChannel().sendMessage("Say `!gauntlet` for free practice or `!gauntlet prize` ("
                        + QuizService.ENTRY_FEE + " GC entry, held until the run ends).").queue();
                return;
            }
            try {
                Object r = QuizService.start(event.getAuthor().getIdLong(),
                        prize ? QuizService.Mode.PRIZE : QuizService.Mode.PRACTICE, null,
                        event.getChannel().getIdLong(), event.getMessageId());
                if (r instanceof String) {
                    event.getChannel().sendMessage((String) r).queue();
                    return;
                }
                if (prize) event.getChannel().sendMessage("Entry of " + QuizService.ENTRY_FEE
                        + " GC reserved. Maximum loss: " + QuizService.ENTRY_FEE + " GC. Clear all "
                        + QuizService.QUESTIONS + " for this week's badge.").queue();
                ask(event.getChannel(), (QuizService.Session) r);
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }

    /** {@code !coop}: weekend server-wide arcade target. */
    public static class Coop extends Command {
        static final int TARGET = 150;

        public Coop() { super("!coop", true); }

        @Override
        public void start(MessageReceivedEvent event) {
            try {
                java.time.ZoneId zone = java.time.ZoneId.of("America/New_York");
                java.time.LocalDate friday = java.time.LocalDate.now(zone)
                        .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.FRIDAY));
                long from = friday.atStartOfDay(zone).toInstant().toEpochMilli();
                long to = friday.plusDays(3).atStartOfDay(zone).toInstant().toEpochMilli();
                int[] stats = database.Database.inTransaction(c -> {
                    try (java.sql.PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(correct), 0), "
                            + "COUNT(DISTINCT user) FROM arcade_scores WHERE played_at >= ? AND played_at < ?")) {
                        ps.setLong(1, from);
                        ps.setLong(2, to);
                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            return new int[]{rs.getInt(1), rs.getInt(2)};
                        }
                    }
                });
                int pct = Math.min(100, stats[0] * 100 / TARGET);
                StringBuilder bar = new StringBuilder();
                for (int i = 0; i < 20; i++) bar.append(i < pct / 5 ? '█' : '░');
                boolean open = System.currentTimeMillis() >= from && System.currentTimeMillis() < to;
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(stats[0] >= TARGET ? Color.GREEN : Color.YELLOW);
                eb.setTitle("Co-op arcade weekend");
                eb.setDescription("`" + bar + "` " + pct + "%\n**" + stats[0] + " / " + TARGET
                        + "** correct answers across the server");
                eb.addField("Players", Integer.toString(stats[1]), true);
                eb.addField("Window", "Friday to Sunday (Eastern)", true);
                eb.setFooter(open ? "Play with !arcade. Taking part is free. Fund the community unlock with "
                        + "!contribute." : "Opens Friday. Taking part is free.", null);
                event.getChannel().sendMessageEmbeds(eb.build()).queue();
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }

}

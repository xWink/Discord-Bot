package command.commands.economy;

import arcade.Question;
import arcade.QuestionBank;
import command.Command;
import command.ui.Buttons;
import database.Database;
import economy.EconomyService;
import economy.Wagers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Trivia race. {@code !race <stake> [category]} puts the host's stake on a question; the first other player to press
 * the right answer takes it. A wrong answer knocks that player out of the round, and if nobody gets it in 45 seconds
 * the host's stake is released. One round per channel.
 */
public final class Race {

    public static final long LIFETIME_MS = 45 * 1000;

    private static final ConcurrentMap<Long, Round> BY_CHANNEL = new ConcurrentHashMap<>();

    private Race() { }

    /** An open race in one channel. */
    public static final class Round {
        public final Question question;
        public final long host;
        public final int stake;
        public final String reservation;
        public final long channelId;
        final long startedAt = System.currentTimeMillis();
        final Set<Long> out = new HashSet<>();

        Round(Question question, long host, int stake, String reservation, long channelId) {
            this.question = question;
            this.host = host;
            this.stake = stake;
            this.reservation = reservation;
            this.channelId = channelId;
        }
    }

    /** @return the race open in a channel, or null */
    public static Round current(long channelId) {
        return BY_CHANNEL.get(channelId);
    }

    /**
     * Releases the host's stake on every race nobody won in time.
     *
     * @return the races that expired, for announcing
     */
    public static List<Round> sweep() {
        List<Round> expired = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<Long, Round> e : BY_CHANNEL.entrySet()) {
            Round r = e.getValue();
            if (now - r.startedAt <= LIFETIME_MS) continue;
            synchronized (r) {
                if (!BY_CHANNEL.remove(e.getKey(), r)) continue;
                try {
                    EconomyService.getInstance().refundReservation(r.reservation);
                    expired.add(r);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return expired;
    }

    /** {@code !race <stake> [category]}. */
    public static class Start extends Command {

        public Start() {
            super("!race", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().startsWith(getKey());
        }

        @Override
        public void start(MessageReceivedEvent event) {
            long host = event.getAuthor().getIdLong();
            long channelId = event.getChannel().getIdLong();
            String[] args = event.getMessage().getContentRaw().trim().split("\\s+");
            int stake = 0;
            try {
                if (args.length >= 2) stake = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                stake = 0;
            }
            if (stake < 1) {
                event.getChannel().sendMessage("Say `!race <stake> [category]`. You put up the stake on a trivia question; "
                        + "the first player to press the right answer wins it. If nobody does in " + LIFETIME_MS / 1000
                        + " seconds you get it back.").queue();
                return;
            }
            try {
                if (BY_CHANNEL.containsKey(channelId)) {
                    event.getChannel().sendMessage("This channel already has a race running.").queue();
                    return;
                }
                String blocked = Wagers.gate(host, stake);
                if (blocked != null) {
                    event.getChannel().sendMessage(blocked).queue();
                    return;
                }
                String category = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : null;
                Question q = category == null ? QuestionBank.next("trivia", null, host)
                        : QuestionBank.nextMatching("trivia", category, host);
                if (q == null) {
                    event.getChannel().sendMessage("No trivia question is available"
                            + (category == null ? "." : " for `" + category + "`.")).queue();
                    return;
                }
                String reservation = "race:" + event.getMessageId();
                try {
                    EconomyService.getInstance().reserve(host, stake, "race", event.getMessageId(), reservation);
                } catch (economy.InsufficientFundsException e) {
                    event.getChannel().sendMessage("You do not have " + stake + " GC spendable.").queue();
                    return;
                }
                Round round = new Round(q, host, stake, reservation, channelId);
                if (BY_CHANNEL.putIfAbsent(channelId, round) != null) {
                    EconomyService.getInstance().refundReservation(reservation);
                    event.getChannel().sendMessage("This channel already has a race running.").queue();
                    return;
                }
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Color.ORANGE);
                eb.setTitle("🏁 Trivia race for " + stake + " GC · " + q.getCategory());
                eb.setDescription(q.getPrompt());
                eb.addField("Choices", Arcade.choices(q.getChoices()), false);
                eb.addField("Rules", "First correct answer takes the pot. A wrong answer knocks you out. "
                        + LIFETIME_MS / 1000 + " seconds. " + event.getAuthor().getName() + " cannot answer.", false);
                eb.setFooter("Host's maximum loss: " + stake + " GC", null);
                event.getChannel().sendMessageEmbeds(eb.build())
                        .setActionRow(Buttons.answers(0, false, "!rans", q.getChoices().size())).queue();
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }

    /** {@code !rans <letter>}. */
    public static class Answer extends Command {

        public Answer() {
            super("!rans", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().startsWith(getKey());
        }

        @Override
        public void start(MessageReceivedEvent event) {
            String[] a = event.getMessage().getContentRaw().trim().split("\\s+");
            if (a.length != 2 || a[1].length() != 1 || !Character.isLetter(a[1].charAt(0))) return;
            long channelId = event.getChannel().getIdLong();
            long user = event.getAuthor().getIdLong();
            Round round = BY_CHANNEL.get(channelId);
            if (round == null) return;
            try {
                synchronized (round) {
                    if (BY_CHANNEL.get(channelId) != round) return;
                    if (System.currentTimeMillis() - round.startedAt > LIFETIME_MS) return; // the sweep will refund it
                    if (user == round.host) {
                        event.getChannel().sendMessage("You put up the stake, so you cannot answer your own race.").queue();
                        return;
                    }
                    if (round.out.contains(user)) return;
                    int choice = Character.toUpperCase(a[1].charAt(0)) - 'A';
                    String name = economy.Cosmetics.nameWithTitle(user, event.getAuthor().getName());
                    if (choice != round.question.getCorrectIndex()) {
                        round.out.add(user);
                        event.getChannel().sendMessage(name + " is out: not " + Character.toUpperCase(a[1].charAt(0))
                                + ".").queue();
                        return;
                    }
                    BY_CHANNEL.remove(channelId, round);
                    String op = "race:" + event.getMessageId();
                    Database.inTransaction(c -> {
                        EconomyService.settleReservationIn(c, round.reservation, -round.stake, "race");
                        EconomyService.creditIn(c, user, round.stake, "race", Long.toString(round.question.getId()), op);
                        return null;
                    });
                    String explanation = round.question.getExplanation();
                    economy.Cosmetics.reply(event.getChannel(), user, name + " got it first: **"
                            + round.question.getChoices().get(round.question.getCorrectIndex()) + "**. You win "
                            + round.stake + " GC from <@" + round.host + ">!"
                            + (explanation != null && !explanation.isEmpty() ? "\n" + explanation : ""), true);
                }
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }
}

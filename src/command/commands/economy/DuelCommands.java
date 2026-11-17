package command.commands.economy;

import command.Command;
import command.commands.blackjack.Bet;
import duel.Duel;
import duel.DuelMath;
import duel.DuelService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
import java.util.List;

/**
 * Duel chat commands: {@code !duel @user [stake]}, {@code !accept},
 * {@code !decline}, {@code !duelanswer <letter>}, {@code !duelboard}.
 */
public final class DuelCommands {

    private DuelCommands() { }

    public static String describe(DuelService.Outcome o) {
        Duel d = o.duel;
        StringBuilder sb = new StringBuilder();
        sb.append("**Duel over.** Answer: **").append(d.getQuestion().getChoices()
                .get(d.getQuestion().getCorrectIndex())).append("**\n");
        if (o.winner == 0) {
            sb.append("Draw.").append(d.stake > 0 ? " Both stakes were returned." : "");
        } else {
            sb.append("<@").append(o.winner).append("> wins").append(economy.Cosmetics.extras(o.winner).effect);
            if (d.stake > 0) sb.append(" +").append(DuelMath.winnerNet(d.stake)).append(" GC")
                    .append(DuelMath.fee(d.stake) > 0 ? " (after a " + DuelMath.fee(d.stake) + " GC fee)" : "");
            sb.append('.');
        }
        if (o.ratings != null) {
            sb.append("\nRatings: <@").append(d.challenger).append("> ").append(o.ratings[0]).append(" · <@")
                    .append(d.opponent).append("> ").append(o.ratings[1]);
        } else {
            sb.append("\n_Unranked: you have already played this opponent ").append(DuelMath.RANKED_PAIR_DAILY_CAP)
                    .append(" ranked times today._");
        }
        return sb.toString();
    }

    /** {@code !duel @user [stake]} */
    public static class Challenge extends Command {
        public Challenge() { super("!duel", true); }

        @Override
        public boolean keyMatches(String s) { return s.toLowerCase().startsWith("!duel ") || s.equalsIgnoreCase("!duel"); }

        @Override
        public void start(MessageReceivedEvent event) {
            String[] args = event.getMessage().getContentRaw().split("\\s+");
            if (event.getMessage().getMentionedUsers().size() != 1 || args.length < 2 || args.length > 3) {
                event.getChannel().sendMessage("Say `!duel @user [stake]`. Stake 0-" + Bet.MAX_STAKE
                        + " GC; leave it off to play free. Winner takes the pot (1 GC fee from 5 GC stakes up).")
                        .queue();
                return;
            }
            int stake;
            try {
                stake = args.length == 3 ? Integer.parseInt(args[2]) : 0;
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("The stake must be a whole number.").queue();
                return;
            }
            if (stake < 0 || stake > Bet.MAX_STAKE) {
                event.getChannel().sendMessage("Stakes run from 0 to " + Bet.MAX_STAKE + " GC.").queue();
                return;
            }
            net.dv8tion.jda.api.entities.User target = event.getMessage().getMentionedUsers().get(0);
            if (target.isBot()) return;
            Duel d = DuelService.challenge(event.getAuthor().getIdLong(), target.getIdLong(), stake,
                    event.getChannel().getIdLong());
            if (d == null) {
                event.getChannel().sendMessage("One of you is already in a duel, or you challenged yourself.")
                        .queue();
                return;
            }
            event.getChannel().sendMessage(target.getAsMention() + ", " + event.getAuthor().getName()
                    + " challenges you to a " + (stake > 0 ? stake + " GC" : "free") + " duel."
                    + (stake > 0 ? " Maximum loss: " + stake + " GC each. Stakes are held only if you accept." : "")
                    + "\nSay `!accept` or `!decline` within 2 minutes.").queue();
        }
    }

    public static class Accept extends Command {
        public Accept() { super("!accept", true); }

        @Override
        public void start(MessageReceivedEvent event) {
            Duel d = DuelService.pendingFor(event.getAuthor().getIdLong());
            if (d == null) return;
            try {
                String problem = DuelService.accept(d);
                if (problem != null) {
                    event.getChannel().sendMessage("Duel cancelled. " + problem).queue();
                    return;
                }
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Color.YELLOW);
                eb.setTitle("Duel: " + d.getQuestion().getCategory() + " · " + d.getQuestion().getDifficulty());
                eb.setDescription(d.getQuestion().getPrompt());
                eb.addField("Choices", Arcade.choices(d.getQuestion().getChoices()), false);
                eb.addField("Stake", d.stake > 0 ? d.stake + " GC each" : "Free", true);
                eb.addField("Time", arcade.ArcadeService.ROUND_SECONDS + " seconds", true);
                eb.setFooter("Both players: !duelanswer <letter>. Correct and fastest wins.", null);
                event.getChannel().sendMessage("<@" + d.challenger + "> <@" + d.opponent + ">")
                        .embed(eb.build()).queue();
            } catch (Exception e) {
                e.printStackTrace();
                DuelService.cancel(d);
                event.getChannel().sendMessage("Could not start the duel; no coins were taken.").queue();
            }
        }
    }

    public static class Decline extends Command {
        public Decline() { super("!decline", true); }

        @Override
        public void start(MessageReceivedEvent event) {
            Duel d = DuelService.pendingFor(event.getAuthor().getIdLong());
            if (d == null) return;
            DuelService.cancel(d);
            event.getChannel().sendMessage("Duel declined. Nothing was charged.").queue();
        }
    }

    public static class DuelAnswer extends Command {
        public DuelAnswer() { super("!duelanswer", true); }

        @Override
        public boolean keyMatches(String s) { return s.toLowerCase().startsWith("!duelanswer"); }

        @Override
        public void start(MessageReceivedEvent event) {
            String[] args = event.getMessage().getContentRaw().split("\\s+");
            if (args.length != 2 || args[1].length() != 1 || !Character.isLetter(args[1].charAt(0))) return;
            try {
                DuelService.Outcome o = DuelService.answer(event.getAuthor().getIdLong(),
                        Character.toUpperCase(args[1].charAt(0)) - 'A');
                event.getMessage().addReaction("✅").queue();
                if (o != null) event.getChannel().sendMessage(describe(o)).queue();
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }

    public static class Board extends Command {
        public Board() { super("!duelboard", true); }

        @Override
        public void start(MessageReceivedEvent event) {
            try {
                List<DuelService.Standing> board = DuelService.board();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Color.YELLOW);
                eb.setTitle("Duel ratings");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(10, board.size()); i++) {
                    DuelService.Standing s = board.get(i);
                    sb.append(i + 1).append(". <@").append(s.user).append("> — ").append(s.rating)
                            .append(" (").append(s.matches).append(" matches)\n");
                }
                eb.setDescription(board.isEmpty() ? "No one has " + DuelMath.RATED_MATCHES_FOR_RANK
                        + " rated matches yet." : sb.toString());
                for (int i = 0; i < board.size(); i++) {
                    if (board.get(i).user == event.getAuthor().getIdLong()) {
                        eb.addField("You", "Rank " + (i + 1) + ", " + board.get(i).rating
                                + (i > 0 ? ", " + (board.get(i - 1).rating - board.get(i).rating)
                                + " behind the next rank" : ""), false);
                    }
                }
                eb.setFooter("Needs " + DuelMath.RATED_MATCHES_FOR_RANK + " matches. Stake size never changes "
                        + "ratings.", null);
                event.getChannel().sendMessage(eb.build()).queue();
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }
}

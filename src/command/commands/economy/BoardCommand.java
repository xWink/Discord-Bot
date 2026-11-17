package command.commands.economy;

import command.Command;
import economy.SeasonService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

/**
 * {@code !board <arcade|duel|collection|community> [YYYYMM]} shows the live
 * board, or a finished season's snapshot. {@code !optout on|off} hides you
 * from public boards.
 */
public final class BoardCommand {

    private BoardCommand() { }

    public static class Board extends Command {
        public Board() { super("!board", true); }

        @Override
        public boolean keyMatches(String s) { return s.toLowerCase().startsWith("!board"); }

        @Override
        public void start(MessageReceivedEvent event) {
            String[] a = event.getMessage().getContentRaw().split("\\s+");
            if (a.length < 2 || !Arrays.asList(SeasonService.BOARDS).contains(a[1].toLowerCase())) {
                event.getChannel().sendMessage("Say `!board <" + String.join("|", SeasonService.BOARDS)
                        + "> [YYYYMM]`. Add a season like `202611` to see a finished month.").queue();
                return;
            }
            String board = a[1].toLowerCase();
            try {
                int season = SeasonService.currentSeason();
                boolean finished = false;
                if (a.length >= 3) {
                    season = Integer.parseInt(a[2]);
                    finished = season < SeasonService.currentSeason();
                }
                List<SeasonService.Row> rows = finished ? SeasonService.snapshot(board, season)
                        : SeasonService.live(board, season);
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Color.YELLOW);
                eb.setTitle(title(board) + " · " + (board.equals("arcade") ? "season " + season : "current")
                        + (finished ? " (final)" : ""));
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(SeasonService.TOP, rows.size()); i++) {
                    SeasonService.Row r = rows.get(i);
                    sb.append(r.rank).append(". <@").append(r.user).append("> — ").append(r.score)
                            .append(unit(board)).append('\n');
                }
                eb.setDescription(rows.isEmpty() ? "Nothing to show yet." : sb.toString());
                for (int i = 0; i < rows.size(); i++) {
                    if (rows.get(i).user == event.getAuthor().getIdLong()) {
                        eb.addField("You", "Rank " + rows.get(i).rank + " · " + rows.get(i).score + unit(board)
                                + (i > 0 ? " · " + (rows.get(i - 1).score - rows.get(i).score)
                                + " to the next rank" : " · top of the board"), false);
                    }
                }
                eb.setFooter(footer(board) + " Ties are ordered by player ID. `!optout on` hides you.", null);
                event.getChannel().sendMessageEmbeds(eb.build()).queue();
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("Seasons look like `202611`.").queue();
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }

        private static String title(String b) {
            return b.substring(0, 1).toUpperCase() + b.substring(1) + " leaderboard";
        }

        private static String unit(String b) {
            switch (b) {
                case "arcade": return " correct";
                case "duel": return " rating";
                case "collection": return " unlocks";
                default: return " goals";
            }
        }

        private static String footer(String b) {
            switch (b) {
                case "duel": return "Needs 5 rated matches. Stake size never changes ratings.";
                case "collection": return "Cosmetics plus achievements; each unlock counts once.";
                case "community": return "Goals you helped complete.";
                default: return "Season runs on calendar months (Eastern). Winners earn a 30-day badge.";
            }
        }
    }

    public static class OptOut extends Command {
        public OptOut() { super("!optout", true); }

        @Override
        public boolean keyMatches(String s) { return s.toLowerCase().startsWith("!optout"); }

        @Override
        public void start(MessageReceivedEvent event) {
            String[] a = event.getMessage().getContentRaw().split("\\s+");
            try {
                long user = event.getAuthor().getIdLong();
                if (a.length == 2 && (a[1].equalsIgnoreCase("on") || a[1].equalsIgnoreCase("off"))) {
                    SeasonService.setOptOut(user, a[1].equalsIgnoreCase("on"));
                    event.getChannel().sendMessage("Public boards: you are now "
                            + (a[1].equalsIgnoreCase("on") ? "hidden" : "visible") + ". Your personal stats are kept.")
                            .queue();
                } else {
                    event.getChannel().sendMessage("You are " + (SeasonService.isOptedOut(user) ? "hidden from"
                            : "visible on") + " public boards. Say `!optout on` or `!optout off`.").queue();
                }
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }
}

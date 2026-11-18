package command.commands.course;

import command.AdminCommand;
import command.Command;
import command.ui.Buttons;
import command.ui.Reveal;
import command.util.art.CourseArt;
import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import courses.Ratings;
import courses.Ratings.Review;
import courses.Ratings.Summary;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Student ratings of courses. {@code !rate cis1300 3 4 fair marking, heavy labs} gives difficulty and workload (1 is
 * easy or light, 5 is hard or heavy) and an optional comment; {@code !reviews cis1300} shows the scores as an animated
 * chart with the latest comments, each with a Report button.
 */
public final class Reviews {

    private static final int COMMENTS_SHOWN = 5;

    private Reviews() { }

    /** A bar for a score, for example {@code ▰▰▰▱▱}. */
    public static String bar(double score) {
        int filled = (int) Math.round(score);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) sb.append(i <= filled ? '▰' : '▱');
        return sb.toString();
    }

    /** The one-line view of the ratings, for a course entry. */
    public static String line(String code, Summary s) {
        if (s == null) return "No ratings yet. Be the first: `!rate " + code.replace("*", "").toLowerCase() + " <difficulty 1-5> <workload 1-5>`";
        if (!s.enoughToShow()) {
            return s.count + (s.count == 1 ? " rating" : " ratings") + " so far (averages show from " + Ratings.SHOW_AVERAGE_FROM
                    + "). `!rate " + code.replace("*", "").toLowerCase() + " <difficulty> <workload>`";
        }
        return "Difficulty " + bar(s.difficulty) + " **" + String.format("%.1f", s.difficulty) + "**/5 · Workload "
                + bar(s.workload) + " **" + String.format("%.1f", s.workload) + "**/5 · " + s.count + " ratings";
    }

    // -------------------------------------------------------------------- rate

    /** {@code !rate <course> <difficulty> <workload> [comment]} and {@code !rate remove <course>}. */
    public static class Rate extends Command {

        public Rate() {
            super("!rate", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().matches("^!rate(\\s+.*)?$");
        }

        private static String usage() {
            return "Say `!rate <course> <difficulty> <workload> [comment]`, for example `!rate cis1300 3 4 fair marking, "
                    + "heavy labs`. Difficulty and workload run from 1 (easy / light) to 5 (hard / heavy). Rate again to "
                    + "change yours, `!rate remove cis1300` to delete it. Ratings are anonymous.";
        }

        @Override
        public void start(MessageReceivedEvent event) {
            long user = event.getAuthor().getIdLong();
            String[] words = event.getMessage().getContentRaw().trim().split("\\s+");
            try {
                if (words.length >= 3 && words[1].equalsIgnoreCase("remove")) {
                    Course c = CourseCatalog.get().find(words[2]);
                    event.getChannel().sendMessage(c != null && Ratings.remove(user, c.code) ? "Removed your rating of "
                            + c.code.replace("*", "\\*") + "." : "You have no rating of that course.").queue();
                    return;
                }
                if (words.length < 4) {
                    event.getChannel().sendMessage(usage()).queue();
                    return;
                }
                // the course may be written "cis 1300", so try one word, then two
                Course c = CourseCatalog.get().find(words[1]);
                int next = 2;
                if (c == null && words.length >= 5) {
                    c = CourseCatalog.get().find(words[1] + words[2]);
                    next = 3;
                }
                if (c == null) {
                    event.getChannel().sendMessage("I do not know the course `" + words[1] + "`. " + usage()).queue();
                    return;
                }
                int difficulty;
                int workload;
                try {
                    difficulty = Integer.parseInt(words[next]);
                    workload = Integer.parseInt(words[next + 1]);
                } catch (RuntimeException e) {
                    event.getChannel().sendMessage(usage()).queue();
                    return;
                }
                if (difficulty < 1 || difficulty > 5 || workload < 1 || workload > 5) {
                    event.getChannel().sendMessage("Difficulty and workload are each 1 to 5.").queue();
                    return;
                }
                StringBuilder comment = new StringBuilder();
                for (int i = next + 2; i < words.length; i++) comment.append(i > next + 2 ? " " : "").append(words[i]);
                if (comment.length() > Ratings.MAX_COMMENT) {
                    event.getChannel().sendMessage("Keep the comment under " + Ratings.MAX_COMMENT + " characters ("
                            + comment.length() + " now).").queue();
                    return;
                }
                boolean first = Ratings.rate(user, c.code, difficulty, workload, Remind.defang(comment.toString()));
                Summary s = Ratings.summary(c.code);
                event.getChannel().sendMessage((first ? "Thanks! Rated " : "Updated your rating of ") + c.code.replace("*", "\\*")
                        + ": difficulty " + difficulty + ", workload " + workload + ".\n" + line(c.code, s))
                        .setActionRow(Buttons.of(0, false, "!reviews " + c.code, "See the reviews", ButtonStyle.SECONDARY)).queue();
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }

    // ----------------------------------------------------------------- reviews

    /** {@code !reviews <course>}. */
    public static class Show extends Command {

        public Show() {
            super("!reviews", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().matches("^!reviews(\\s+.*)?$");
        }

        @Override
        public void start(MessageReceivedEvent event) {
            String query = event.getMessage().getContentRaw().trim().replaceFirst("(?i)^!reviews\\s*", "").trim();
            if (query.isEmpty()) {
                event.getChannel().sendMessage("Say `!reviews <course>`, for example `!reviews cis1300`.").queue();
                return;
            }
            try {
                Course c = CourseLookup.resolve(event.getChannel(), query, "!reviews");
                if (c == null) return;
                Summary s = Ratings.summary(c.code);
                if (s == null) {
                    event.getChannel().sendMessage("Nobody has rated " + c.code.replace("*", "\\*") + " yet. Say `!rate "
                            + c.code.replace("*", "").toLowerCase() + " <difficulty 1-5> <workload 1-5>` to be the first.").queue();
                    return;
                }
                List<Review> comments = Ratings.comments(c.code, COMMENTS_SHOWN);
                MessageChannel channel = event.getChannel();
                EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0xFEE75C))
                        .setTitle(c.code.replace("*", "\\*") + " · " + c.title)
                        .setDescription(line(c.code, s));
                List<ItemComponent> buttons = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                for (Review r : comments) {
                    sb.append("**#").append(r.id).append("** · difficulty ").append(r.difficulty).append(", workload ")
                            .append(r.workload).append("\n> ").append(r.comment).append("\n");
                    buttons.add(Buttons.of(0, false, "!reportreview " + r.id, "Report #" + r.id, ButtonStyle.SECONDARY));
                }
                if (sb.length() > 0) eb.addField("Latest comments", sb.length() > 1000 ? sb.substring(0, 999) + "…" : sb.toString(), false);
                eb.setFooter("Anonymous · 1 = easy / light, 5 = hard / heavy · Report a comment that is unfair or unkind", null);
                Reveal.async(() -> CourseArt.ratingsChart(c, s),
                        gif -> {
                            net.dv8tion.jda.api.requests.restaction.MessageCreateAction a = channel
                                    .sendFiles(FileUpload.fromData(gif, "ratings.gif"))
                                    .setEmbeds(eb.setImage("attachment://ratings.gif").build());
                            if (!buttons.isEmpty()) a.setComponents(ActionRow.of(buttons));
                            a.queue();
                        },
                        () -> {
                            net.dv8tion.jda.api.requests.restaction.MessageCreateAction a = channel.sendMessageEmbeds(eb.build());
                            if (!buttons.isEmpty()) a.setComponents(ActionRow.of(buttons));
                            a.queue();
                        });
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }

    // ----------------------------------------------------------------- reports

    /** {@code !reportreview <number>}. */
    public static class Report extends Command {

        public Report() {
            super("!reportreview", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().matches("^!reportreview(\\s+.*)?$");
        }

        @Override
        public void start(MessageReceivedEvent event) {
            String[] words = event.getMessage().getContentRaw().trim().split("\\s+");
            long id;
            try {
                id = Long.parseLong(words[1].replace("#", ""));
            } catch (RuntimeException e) {
                event.getChannel().sendMessage("Say `!reportreview <number>`; the number is next to the comment.").queue();
                return;
            }
            try {
                switch (Ratings.report(event.getAuthor().getIdLong(), id)) {
                    case OK:
                        event.getChannel().sendMessage("Thanks, I have noted your report on #" + id + ". After "
                                + Ratings.REPORTS_TO_HIDE + " reports from different people a comment is hidden.").queue();
                        break;
                    case HIDDEN:
                        event.getChannel().sendMessage("Thanks. #" + id + " has enough reports and is now hidden.").queue();
                        break;
                    case OWN:
                        event.getChannel().sendMessage("That one is yours: `!rate remove <course>` deletes it.").queue();
                        break;
                    case ALREADY:
                        event.getChannel().sendMessage("You already reported #" + id + ".").queue();
                        break;
                    default:
                        event.getChannel().sendMessage("There is no visible review #" + id + ".").queue();
                }
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }

    /**
     * {@code !review hide <number>} hides a comment but keeps its scores; {@code !review remove <number>} hides it and
     * takes the scores out of the averages too. For moderators.
     */
    public static class Hide extends Command implements AdminCommand {

        private static final Pattern FORM = Pattern.compile("(?i)^!review\\s+(hide|remove)\\s+#?(\\d+)$");

        public Hide() {
            super("!review", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return FORM.matcher(string.trim()).matches();
        }

        @Override
        public void start(MessageReceivedEvent event) {
            if (!memberIsAdmin(event.getMember())) return;
            java.util.regex.Matcher m = FORM.matcher(event.getMessage().getContentRaw().trim());
            if (!m.matches()) return;
            try {
                long id = Long.parseLong(m.group(2));
                boolean remove = m.group(1).equalsIgnoreCase("remove");
                boolean done = remove ? Ratings.exclude(id) : Ratings.hide(id);
                event.getChannel().sendMessage(!done ? "There is no review #" + id + "."
                        : remove ? "Removed review #" + id + ": its comment is hidden and its scores no longer count toward the averages."
                        : "Hidden review #" + id + ". Its scores still count; `!review remove " + id + "` takes them out too.").queue();
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }
}

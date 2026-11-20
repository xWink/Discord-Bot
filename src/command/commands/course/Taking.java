package command.commands.course;

import command.Command;
import command.ui.Buttons;
import command.util.misc.CourseCatalog;
import command.util.misc.CourseCatalog.Course;
import courses.TakingStore;
import economy.ProfilePrefs;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The courses you are taking now: {@code !taking add cis2750}, {@code !taking done cis2750} when the term ends (it moves
 * to your finished list), and {@code !classmates cis2750} to see who else is taking a course. Nobody is listed as a
 * classmate unless they say {@code !taking share on}.
 */
public final class Taking {

    private Taking() { }

    private static String escape(String text) {
        return text.replace("*", "\\*");
    }

    // ------------------------------------------------------------------ taking

    /** {@code !taking [add|remove|done|list|share] ...}. */
    public static class Manage extends Command {

        public Manage() {
            super("!taking", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().matches("^!taking(\\s+.*)?$");
        }

        @Override
        public void start(MessageReceivedEvent event) {
            long user = event.getAuthor().getIdLong();
            String args = event.getMessage().getContentRaw().trim().replaceFirst("(?i)^!taking\\s*", "").trim();
            String[] words = args.split("\\s+", 2);
            String action = words[0].toLowerCase();
            String rest = words.length > 1 ? words[1] : "";
            try {
                switch (action) {
                    case "add":
                    case "remove":
                    case "done": {
                        List<String> unknown = new ArrayList<>();
                        List<String> codes = Planner.codesIn(rest, unknown);
                        if (action.equals("done") && rest.trim().equalsIgnoreCase("all")) codes = new ArrayList<>(TakingStore.list(user));
                        if (codes.isEmpty()) {
                            event.getChannel().sendMessage("Say `!taking " + action + (action.equals("done") ? " cis2750` (or `all`)" : " cis2750 math1200`")
                                    + "." + (unknown.isEmpty() ? "" : " I do not know " + escape(String.join(", ", unknown)) + ".")).queue();
                            return;
                        }
                        String note = unknown.isEmpty() ? "" : "\nNot in the catalogue: " + escape(String.join(", ", unknown)) + ".";
                        if (action.equals("add")) {
                            int n = TakingStore.add(user, codes);
                            List<ItemComponent> buttons = new ArrayList<>();
                            for (String code : codes) {
                                if (buttons.size() >= 4) break;
                                buttons.add(Buttons.of(0, false, "!classmates " + code, "Who else takes " + code + "?", ButtonStyle.SECONDARY));
                            }
                            event.getChannel().sendMessage("Added " + n + " current course" + (n == 1 ? "" : "s") + ". You are taking "
                                    + TakingStore.list(user).size() + ". Join the course channel with `!join <course>`; see "
                                    + "classmates with `!classmates <course>` (they only show if they said `!taking share on`)." + note)
                                    .setComponents(ActionRow.of(buttons)).queue();
                        } else if (action.equals("remove")) {
                            int n = TakingStore.remove(user, codes);
                            event.getChannel().sendMessage("Removed " + n + " course" + (n == 1 ? "" : "s") + "." + note).queue();
                        } else {
                            int n = TakingStore.finish(user, codes);
                            event.getChannel().sendMessage(n == 0 ? "None of those are in your current courses." + note
                                    : "Moved " + n + " course" + (n == 1 ? "" : "s") + " to your finished list. Well done! `!next` shows what that opens up."
                                    + note).setActionRow(Buttons.of(user, false, "!next", "What can I take next?", ButtonStyle.PRIMARY)).queue();
                        }
                        return;
                    }
                    case "share": {
                        String v = rest.trim().toLowerCase();
                        if (!v.equals("on") && !v.equals("off")) {
                            event.getChannel().sendMessage("Sharing is **" + (ProfilePrefs.isSharingClasses(user) ? "on" : "off")
                                    + "**: " + (ProfilePrefs.isSharingClasses(user) ? "classmates can see you in `!classmates`." : "nobody sees which "
                                    + "courses you take.") + " Say `!taking share on` or `!taking share off`.").queue();
                            return;
                        }
                        ProfilePrefs.setSharingClasses(user, v.equals("on"));
                        event.getChannel().sendMessage(v.equals("on") ? "Done: people can now see you in `!classmates` for the courses you list."
                                : "Done: you no longer show in `!classmates`.").queue();
                        return;
                    }
                    case "":
                    case "list": {
                        Set<String> taking = TakingStore.list(user);
                        if (taking.isEmpty()) {
                            event.getChannel().sendMessage("You have not listed any current courses. `!taking add cis2750 math1200`.").queue();
                            return;
                        }
                        CourseCatalog catalog = CourseCatalog.get();
                        StringBuilder sb = new StringBuilder();
                        List<ItemComponent> buttons = new ArrayList<>();
                        for (String code : taking) {
                            Course c = catalog.find(code);
                            sb.append("**").append(escape(code)).append("** ").append(c == null ? "" : c.title).append('\n');
                            if (buttons.size() < 5) buttons.add(Buttons.of(user, false, "!taking done " + code, "Finished " + code, ButtonStyle.SUCCESS));
                        }
                        EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0x3498DB)).setTitle("Taking now (" + taking.size() + ")")
                                .setDescription(sb.toString())
                                .setFooter("Sharing: " + (ProfilePrefs.isSharingClasses(user) ? "on" : "off") + " · !taking share on|off · "
                                        + "press Finished when the term ends", null);
                        event.getChannel().sendMessageEmbeds(eb.build()).setComponents(ActionRow.of(buttons)).queue();
                        return;
                    }
                    default:
                        event.getChannel().sendMessage("Say `!taking add cis2750`, `!taking remove cis2750`, `!taking done cis2750`, "
                                + "`!taking list` or `!taking share on|off`.").queue();
                }
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }

    // --------------------------------------------------------------- classmates

    /** {@code !classmates <course>}. */
    public static class Classmates extends Command {

        public Classmates() {
            super("!classmates", true);
        }

        @Override
        public boolean keyMatches(String string) {
            return string.toLowerCase().matches("^!classmates(\\s+.*)?$");
        }

        @Override
        public void start(MessageReceivedEvent event) {
            String query = event.getMessage().getContentRaw().trim().replaceFirst("(?i)^!classmates\\s*", "").trim();
            if (query.isEmpty()) {
                event.getChannel().sendMessage("Say `!classmates <course>`, for example `!classmates cis2750`.").queue();
                return;
            }
            try {
                Course c = CourseLookup.resolve(event.getChannel(), query, "!classmates");
                if (c == null) return;
                TakingStore.Classmates found = TakingStore.classmates(c.code, 25);
                if (found.total == 0) {
                    event.getChannel().sendMessage("Nobody has listed " + escape(c.code) + " as a current course yet. "
                            + "`!taking add " + c.code.replace("*", "").toLowerCase() + "` adds it for you.").queue();
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (long id : found.visible) sb.append("<@").append(id).append("> ");
                int hidden = found.total - found.visible.size();
                EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0x3498DB)).setTitle(escape(c.code) + " · " + c.title)
                        .setDescription(found.total + (found.total == 1 ? " person is" : " people are") + " taking this now."
                                + (sb.length() > 0 ? "\n" + sb : "")
                                + (hidden > 0 ? "\n" + hidden + " " + (hidden == 1 ? "has" : "have") + " not chosen to be shown." : ""))
                        .setFooter("Only people who said !taking share on appear here", null);
                event.getChannel().sendMessageEmbeds(eb.build()).setComponents(ActionRow.of(
                        Buttons.of(0, false, "!join " + c.department + c.number, "Join the " + c.department + c.number + " channel", ButtonStyle.PRIMARY),
                        Buttons.of(0, false, "!info " + c.code, "Course info", ButtonStyle.SECONDARY))).queue();
            } catch (Exception e) {
                printStackTraceAndSendMessage(event, e);
            }
        }
    }
}

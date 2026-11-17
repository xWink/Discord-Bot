package command.commands.economy;

import command.AdminCommand;
import command.Command;
import command.util.art.Sticker;
import economy.AlumniService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Alumni, an honour mods bestow. {@code !alumni} shows your own status and {@code !alumni list} shows everyone;
 * {@code !alumni grant @user [year] [note]} (the year is optional) and {@code !alumni revoke @user} are for mods only.
 */
public class AlumniCommand extends Command implements AdminCommand {

    private static final String ROLE_NAME = "Alumni";

    public AlumniCommand() {
        super("!alumni", true);
    }

    @Override
    public boolean keyMatches(String s) {
        return s.toLowerCase().startsWith("!alumni");
    }

    @Override
    public void start(MessageReceivedEvent event) {
        String[] a = event.getMessage().getContentRaw().split("\\s+");
        AlumniService svc = AlumniService.getInstance();
        try {
            String sub = a.length >= 2 ? a[1].toLowerCase() : "";
            if (sub.equals("grant") || sub.equals("revoke")) {
                if (!memberIsAdmin(event.getMember())) {
                    event.getChannel().sendMessage("Only mods can " + sub + " alumni.").queue();
                    return;
                }
                if (event.getMessage().getMentions().getUsers().size() != 1) {
                    event.getChannel().sendMessage("Say `!alumni " + sub + " @user"
                            + (sub.equals("grant") ? " [year] [note]" : "") + "`. The year is optional, e.g. `2019`.").queue();
                    return;
                }
                User target = event.getMessage().getMentions().getUsers().get(0);
                if (sub.equals("grant")) grant(event, svc, target, a);
                else revoke(event, svc, target);
            } else if (sub.equals("list")) {
                list(event, svc);
            } else {
                AlumniService.Record r = svc.get(event.getAuthor().getIdLong());
                event.getChannel().sendMessage(r == null
                        ? "You are not alumni. Mods award it to members who have moved on. See `!alumni list`."
                        : "🎓 You are alumni" + (r.classYear() > 0 ? ", " + r.classLabel().toLowerCase() : "")
                        + (r.note != null ? ": " + r.note : "") + ".").queue();
            }
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    private void grant(MessageReceivedEvent event, AlumniService svc, User target, String[] a) throws Exception {
        // !alumni grant @user [year] [note]: a 4-digit year is optional, anything after it (or after the mention) is the note
        int year = 0;
        int noteStart = 3;
        if (a.length > 3 && a[3].matches("(19|20)\\d{2}")) {
            year = Integer.parseInt(a[3]);
            noteStart = 4;
        }
        String note = a.length > noteStart ? String.join(" ", Arrays.copyOfRange(a, noteStart, a.length)) : "";
        if (note.length() > 200) note = note.substring(0, 200);
        boolean isNew = svc.grant(target.getIdLong(), event.getAuthor().getIdLong(), note, year);

        // Give the Discord role too, if the server has one named "Alumni".
        List<Role> roles = event.getGuild().getRolesByName(ROLE_NAME, true);
        if (!roles.isEmpty()) {
            event.getGuild().addRoleToMember(target, roles.get(0)).queue(ok -> { }, fail -> { });
        }

        AlumniService.Record r = svc.get(target.getIdLong());
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Sticker.GOLD);
        eb.setTitle("🎓 Welcome to the alumni!");
        eb.setDescription(target.getAsMention() + (isNew ? " is now an alumnus" : " is alumni")
                + (r.classYear() > 0 ? ", " + r.classLabel().toLowerCase() : "") + "."
                + (r.note != null ? "\n" + r.note : ""));
        eb.setFooter("Shown on your !profile and next to your name. Nothing to equip.", null);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(Sticker.render(Collections.singletonList("alumni"), 256, 3), "png", out);
            eb.setThumbnail("attachment://alumni.png");
            event.getChannel().sendFiles(FileUpload.fromData(out.toByteArray(), "alumni.png"))
                    .setEmbeds(eb.build()).queue();
        } catch (Exception e) {
            event.getChannel().sendMessageEmbeds(eb.build()).queue();
        }
    }

    private void revoke(MessageReceivedEvent event, AlumniService svc, User target) throws Exception {
        boolean was = svc.revoke(target.getIdLong());
        List<Role> roles = event.getGuild().getRolesByName(ROLE_NAME, true);
        if (was && !roles.isEmpty()) {
            event.getGuild().removeRoleFromMember(target, roles.get(0)).queue(ok -> { }, fail -> { });
        }
        event.getChannel().sendMessage(was ? target.getAsMention() + " is no longer listed as alumni."
                : target.getAsMention() + " was not alumni.").queue();
    }

    private void list(MessageReceivedEvent event, AlumniService svc) throws Exception {
        List<AlumniService.Record> all = svc.list(25);
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Sticker.GOLD);
        eb.setTitle("🎓 Alumni");
        StringBuilder sb = new StringBuilder();
        for (AlumniService.Record r : all) {
            sb.append("<@").append(r.user).append(">").append(r.classYear() > 0 ? " · " + r.classLabel().toLowerCase() : "")
                    .append(r.note != null ? " — " + r.note : "").append('\n');
        }
        eb.setDescription(all.isEmpty() ? "No alumni yet." : sb.toString());
        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }
}

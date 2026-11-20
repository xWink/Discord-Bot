package command.commands.admin;

import command.AdminCommand;
import command.Command;
import command.ui.Reveal;
import command.ui.Stats;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;

/** {@code !botstatus}, for moderators: how long the bot has been up, what it has done, how the animations are doing, and the last errors. */
public class BotStatus extends Command implements AdminCommand {

    public BotStatus() {
        super("!botstatus", true);
    }

    @Override
    public void start(MessageReceivedEvent event) {
        if (!memberIsAdmin(event.getMember())) return;
        event.getChannel().sendMessageEmbeds(embed(Stats.snapshot(), pool(), ping()).build()).queue();
    }

    private static String ping() {
        try {
            return main.Server.API == null ? "unknown" : main.Server.API.getGatewayPing() + " ms";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String pool() {
        try {
            com.zaxxer.hikari.HikariPoolMXBean bean = database.Database.getPool().getHikariPoolMXBean();
            return bean == null ? "not started" : bean.getActiveConnections() + " busy, " + bean.getIdleConnections() + " idle, "
                    + bean.getThreadsAwaitingConnection() + " waiting";
        } catch (Exception e) {
            return "unreachable";
        }
    }

    /** The status as an embed; separate from the command so a test can check it without Discord. */
    static EmbedBuilder embed(Stats.Snapshot s, String database, String ping) {
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long max = rt.maxMemory() / (1024 * 1024);
        EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0x57F287)).setTitle("Bot status");
        eb.addField("Up", Stats.duration(s.uptimeMs), true);
        eb.addField("Gateway ping", ping, true);
        eb.addField("Memory", used + " / " + max + " MB", true);
        eb.addField("Commands run", s.commandsRun + (s.topCommands.isEmpty() ? "" : "\n" + String.join(", ", s.topCommands)), false);
        eb.addField("Animations", s.renders + " drawn · avg " + Math.round(s.renderAvgMs) + " ms · slowest " + s.renderMaxMs
                + " ms\n" + Reveal.drawing() + " drawing now, " + Reveal.queued() + " waiting · " + s.renderFailed + " failed, "
                + s.renderRejected + " dropped when busy · " + s.reducedMotion + " skipped for people with motion off", false);
        eb.addField("Database connections", database, false);
        eb.addField("Recent errors", s.recentErrors.isEmpty() ? "None since the bot started." : String.join("\n", s.recentErrors), false);
        if (s.renderRejected > 0 || s.renderFailed > 0 || !s.recentErrors.isEmpty()) eb.setColor(new Color(0xFEE75C));
        return eb;
    }
}

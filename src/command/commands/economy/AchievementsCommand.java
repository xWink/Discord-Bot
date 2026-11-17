package command.commands.economy;

import command.Command;
import economy.Achievements;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
import java.util.List;
import java.util.Map;

public class AchievementsCommand extends Command {

    public AchievementsCommand() {
        super("!achievements", true);
    }

    @Override
    public void start(MessageReceivedEvent event) {
        try {
            List<String> have = Achievements.unlocked(event.getAuthor().getIdLong());
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : Achievements.ALL.entrySet()) {
                sb.append(have.contains(e.getKey()) ? "✅ " : "⬜ ").append(e.getValue()).append('\n');
            }
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.YELLOW);
            eb.setTitle(event.getAuthor().getName() + "'s achievements (" + have.size() + "/" + Achievements.ALL.size() + ")");
            eb.setDescription(sb.toString());
            event.getChannel().sendMessageEmbeds(eb.build()).queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

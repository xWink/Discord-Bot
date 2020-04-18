package command.commands.admin;

import command.AdminCommand;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Objects;

public class DeleteCourseChannels extends AdminCommand {

    /**
     * Initializes the command's key to "!deletecoursechannels"
     */
    public DeleteCourseChannels() {
        super("!deletecoursechannels", true);
    }

    @Override
    protected void runCommand(MessageReceivedEvent event) {
        Category courses = Objects.requireNonNull(event.getGuild().getCategoriesByName("courses", true).get(0));
        for (TextChannel channel : courses.getTextChannels())
            channel.delete().queue();
    }
}

package command.commands.admin;

import command.AdminCommand;
import command.Command;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Objects;

public class DeleteCourseChannels extends Command implements AdminCommand {

    /**
     * Initializes the command's key to "!deletecoursechannels"
     */
    public DeleteCourseChannels() {
        super("!deletecoursechannels", true);
    }

    @Override
    public void start(MessageReceivedEvent event) {
        if (!AdminCommand.memberIsAdmin(event.getMember()))
            return;

        Category courses = Objects.requireNonNull(event.getGuild().getCategoriesByName("your channels", true).get(0));
        for (TextChannel channel : courses.getTextChannels())
            channel.delete().queue();
    }
}

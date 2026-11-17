package main.eventlisteners;

import command.commands.Help;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Switches the help embed when its drop-down changes. Only the person who
 * asked for the menu can use it.
 */
public class HelpMenuListener extends ListenerAdapter {

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("help:")) return;
        if (!id.equals("help:" + event.getUser().getId())) {
            event.reply("That menu belongs to someone else. Say `!help` to open your own.").setEphemeral(true).queue();
            return;
        }
        Help.Category category = Help.Category.find(event.getValues().get(0));
        if (category == null) category = Help.Category.OVERVIEW;
        event.editMessageEmbeds(category.page().build())
                .setActionRow(Help.menu(event.getUser().getIdLong(), category)).queue();
    }
}

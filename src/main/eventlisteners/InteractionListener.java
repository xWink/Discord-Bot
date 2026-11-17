package main.eventlisteners;

import command.ui.Buttons;
import command.ui.CommandDispatcher;
import command.ui.SlashCommands;
import command.ui.SyntheticMessage;
import main.Server;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Collections;

/**
 * Registers the slash commands and runs slash commands and buttons by turning
 * them into the equivalent text command (see {@link SyntheticMessage}).
 */
public class InteractionListener extends ListenerAdapter {

    /** Publishes the slash commands to the server (instant), or globally if the server is not cached. */
    @Override
    public void onReady(ReadyEvent event) {
        JDA jda = event.getJDA();
        Guild guild = jda.getGuildById(Server.GUILD_ID);
        try {
            if (guild != null) {
                guild.updateCommands().addCommands(SlashCommands.all()).queue(
                        ok -> System.out.println("Registered " + ok.size() + " slash commands"),
                        Throwable::printStackTrace);
            } else {
                jda.updateCommands().addCommands(SlashCommands.all()).queue(null, Throwable::printStackTrace);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        SlashCommands.Translation t = SlashCommands.translate(event);
        // The command's own replies go to the channel. The visible slash reply stays, showing what ran,
        // so the channel reads "<user> used /bet" followed by the bot's answer.
        event.deferReply().queue(hook -> {
            try {
                MessageReceivedEvent synthetic = SyntheticMessage.event(event.getJDA(), event.getIdLong(), t.text,
                        event.getUser(), event.getMember(), event.getChannel(), t.users, t.roles, t.members);
                if (!CommandDispatcher.dispatch(synthetic)) {
                    hook.editOriginal("That command is not available right now.").queue();
                    return;
                }
                hook.editOriginal("Ran `" + t.text + "`").queue();
            } catch (Exception e) {
                e.printStackTrace();
                hook.editOriginal("Something went wrong running that. Please try again.").queue();
            }
        });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        Buttons.Parsed p = Buttons.parseId(event.getComponentId());
        if (p == null) return;
        long who = event.getUser().getIdLong();
        if (p.allowed() != 0 && p.allowed() != who) {
            event.reply("Those buttons are not for you. Say the command yourself, or use `/help`.")
                    .setEphemeral(true).queue();
            return;
        }
        if (p.consume()) event.editComponents(Collections.emptyList()).queue();
        else event.deferEdit().queue();
        try {
            Member member = event.getMember();
            MessageReceivedEvent synthetic = SyntheticMessage.event(event.getJDA(), event.getIdLong(), p.text(),
                    event.getUser(), member, event.getChannel(), Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList());
            CommandDispatcher.dispatch(synthetic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

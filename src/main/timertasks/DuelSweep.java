package main.timertasks;

import command.commands.economy.DuelCommands;
import duel.DuelService;
import main.Server;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.TimerTask;

/** Expires unanswered challenges and finishes duels whose clock ran out. */
public class DuelSweep extends TimerTask {

    @Override
    public void run() {
        for (DuelService.Outcome o : DuelService.sweep()) {
            TextChannel channel = Server.API.getTextChannelById(o.duel.channelId);
            if (channel != null) channel.sendMessage(DuelCommands.describe(o)).queue();
        }
    }
}

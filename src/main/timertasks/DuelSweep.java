package main.timertasks;

import command.commands.economy.DuelCommands;
import duel.DuelService;
import main.Server;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.TimerTask;

/** Expires unanswered challenges and finishes duels whose clock ran out. */
public class DuelSweep extends TimerTask {

    @Override
    public void run() {
        for (java.util.Map.Entry<Long, arcade.PersonalTrivia.Round> e : arcade.PersonalTrivia.sweep().entrySet()) {
            TextChannel channel = Server.API.getTextChannelById(e.getKey());
            arcade.Question q = e.getValue().question;
            if (channel != null) channel.sendMessage("Round over. The answer was **" + q.getChoices().get(q.getCorrectIndex())
                    + "** (" + arcade.PersonalTrivia.participants(e.getValue()) + " played).").queue();
        }
        for (command.commands.economy.Race.Round r : command.commands.economy.Race.sweep()) {
            TextChannel channel = Server.API.getTextChannelById(r.channelId);
            if (channel != null) channel.sendMessage("Nobody got the race. The answer was **"
                    + r.question.getChoices().get(r.question.getCorrectIndex()) + "**. <@" + r.host
                    + "> gets their " + r.stake + " GC back.").queue();
        }
        for (arcade.QuizService.Session s : arcade.QuizService.sweep()) {
            TextChannel channel = Server.API.getTextChannelById(s.channelId);
            if (channel != null) channel.sendMessage("<@" + s.user + "> your run timed out"
                    + (s.reservation != null ? " and the entry fee was refunded." : ".")).queue();
        }
        for (DuelService.Outcome o : DuelService.sweep()) {
            TextChannel channel = Server.API.getTextChannelById(o.duel.channelId);
            if (channel != null) economy.Cosmetics.reply(channel, o.winner, DuelCommands.describe(o), o.winner != 0);
        }
    }
}

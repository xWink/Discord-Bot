package DiscordBot.commands;

import net.dv8tion.jda.core.entities.MessageChannel;

public class Help {

    public static void help(MessageChannel channel){

        channel.sendMessage(
                "`!join` & `!leave` - Instructions for joining or leaving a group\n\n" +
                    "`!roles` - See available groups\n\n" +
                    "`!bang` - Play Russian Roulette\n\n" +
                    "`!bangscores` & `!mybang` - See Bang high scores or personal stats").queue();
    }
}

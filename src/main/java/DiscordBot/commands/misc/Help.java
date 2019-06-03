package DiscordBot.commands.misc;

import net.dv8tion.jda.core.entities.TextChannel;

public class Help {

    public static void help(TextChannel channel){

        channel.sendMessage(
                "`!join` & `!leave` - Join or leave a channel for your electives\n\n" +
                    "`!roles` - See available groups\n\n" +
                    "`!bang` - Play Russian Roulette\n\n" +
                    "`!bangscores` & `!mybang` - See bang high scores or personal stats\n\n" +
                    "`!bet`, `!hit`, `!hand`, & `!stand` - Play blackjack\n\n" +
                    "`!wallet` - Show how many GryphCoins you have\n\n" +
                    "`!market` & `!buy <item #>` - Use the GryphCoin market").queue();
    }
}

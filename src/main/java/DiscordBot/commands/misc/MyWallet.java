package DiscordBot.commands.misc;

import DiscordBot.util.wallet.Wallet;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.sql.Connection;

public class MyWallet {

    public static void myWallet (User author, MessageChannel channel, Connection conn){

        Wallet wallet = new Wallet(author, conn);
        channel.sendMessage(author.getName() + " has " + wallet.getWealth() + " GryphCoins").complete();
    }
}

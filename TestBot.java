package DiscordBots;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

public class TestBot {
    public static void main(String[] args) throws Exception{
        try {
            // Create bot with token given by Discord developer page
            JDA api = new JDABuilder(AccountType.BOT).setToken("CHANGETOKEN").buildAsync();
            api.addEventListener(new MyEventListener());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

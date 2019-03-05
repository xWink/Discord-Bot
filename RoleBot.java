package DiscordBots;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

public class RoleBot {
    public static void main(String[] args){
            try {
            // Create bot with token given by Discord developer page
            JDA api = new JDABuilder(AccountType.BOT).setToken("NTQ2MDUyOTE0MDQ1NTE3ODQ1.D14DHA.51ktfTFt1eU8fphFdwrCL4f-Y4Y").build();
            api.addEventListener(new DiscordBots.MyEventListener());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

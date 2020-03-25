package main;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

/**
 * Contains critical details about the server based on information in the config file.
 */
public final class Server {

    private static JDA api;
    public static final long GENERAL_CHANNEL_ID = 486633949154770946L;
    public static final long BOTS_CHANNEL_ID = 551828950871965696L;
    public static final long SPAM_CHANNEL_ID = 674369527731060749L;
    public static final long GUILD_ID = Long.parseLong(Config.getGuildId());

    private Server() {}

    static {
        try {
            api = new JDABuilder(AccountType.BOT)
                    .setToken(Config.getToken())
                    .setBulkDeleteSplittingEnabled(false)
                    .build();

            api.getPresence().setActivity(Activity.playing("!help"));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Returns the JDA object which was built from the bot's token.
     *
     * @return JDA's API
     */
    public static JDA getApi() {
        return api;
    }
}

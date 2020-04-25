package main;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

/**
 * Contains critical details about the server.
 */
public final class Server {

    public static JDA API;
    public static final long GUILD_ID = Long.parseLong(Config.getGuildId());

    // Channels
    public static final long GENERAL_CHANNEL_ID = 486633949154770946L;
    public static final long BOTS_CHANNEL_ID = 551828950871965696L;
    public static final long SPAM_CHANNEL_ID = 674369527731060749L;
    public static final long CHANNELS_CHANNEL_ID = 699595118583480331L;

    // Messages
    public static final long WELCOME_MESSAGE_ID = 699594773480079430L;

    // Emojis
    public static final long CHECK_EMOJI_ID = 699594880527106079L;

    // Roles
    public static final long ADMIN_ROLE_ID = 486635066928136194L;
    public static final long JANITOR_ROLE_ID = 622128124313862164L;
    public static final long TOS_ROLE_ID = 701068412490874890L;

    private Server() {}

    static {
        try {
            API = new JDABuilder(AccountType.BOT)
                    .setToken(Config.getToken())
                    .setBulkDeleteSplittingEnabled(false)
                    .build();

            API.getPresence().setActivity(Activity.playing("!help"));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

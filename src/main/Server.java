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
    public static final long GENERAL_CHANNEL_ID = 546092348069642251L;
    public static final long BOTS_CHANNEL_ID = 551828950871965696L;
    public static final long SPAM_CHANNEL_ID = 674369527731060749L;
    public static final long CHANNELS_CHANNEL_ID = 692330710614409269L;

    // Messages
    public static final long WELCOME_MESSAGE_ID = 692310650172735530L;

    // Emojis
    public static final long CHECK_EMOJI_ID = 692324735668846653L;

    // Roles
    public static final long TOS_ROLE_ID = 692309516435587142L;

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

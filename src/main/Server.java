package main;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

/**
 * Contains critical details about the server.
 */
public final class Server {

    public static JDA API;
    public static final long GUILD_ID = Long.parseLong(Config.getGuildId());

    // LIVE SERVER
    // Channels
    public static final long GENERAL_CHANNEL_ID = Config.getId("GENERAL_CHANNEL", 486633949154770946L);
    public static final long BOTS_CHANNEL_ID = Config.getId("BOTS_CHANNEL", 551828950871965696L);
    public static final long SPAM_CHANNEL_ID = Config.getId("SPAM_CHANNEL", 674369527731060749L);
    public static final long CHANNELS_CHANNEL_ID = Config.getId("CHANNELS_CHANNEL", 699595118583480331L);
    public static final long DELETED_MESSAGES_CHANNEL_ID = Config.getId("DELETED_MESSAGES_CHANNEL", 677109914400980992L);
    public static final long EDITED_MESSAGES_CHANNEL_ID = Config.getId("EDITED_MESSAGES_CHANNEL", 738143259158904992L);

    // Messages
    public static final long WELCOME_MESSAGE_ID = Config.getId("WELCOME_MESSAGE", 699594773480079430L);

    // Emojis
    public static final long CHECK_EMOJI_ID = Config.getId("CHECK_EMOJI", 699594880527106079L);
    public static final long JAM_EMOJI_ID = Config.getId("JAM_EMOJI", 554666728878112774L);
    public static final long SURVIVE_EMOJI_ID = Config.getId("SURVIVE_EMOJI", 564285288621539328L);

    // Roles
    public static final long ADMIN_ROLE_ID = Config.getId("ADMIN_ROLE", 486635066928136194L);
    public static final long JANITOR_ROLE_ID = Config.getId("JANITOR_ROLE", 622128124313862164L);
    public static final long TOS_ROLE_ID = Config.getId("TOS_ROLE", 701068412490874890L);
    public static final long ALL_ROLE_ID = Config.getId("ALL_ROLE", 703608427036934235L);
    public static final long NOTIFY_ROLE_ID = Config.getId("NOTIFY_ROLE", 707756300087590992L);

    // TEST SERVER
    // Channels
//    public static final long GENERAL_CHANNEL_ID = 546092348069642251L;
//    public static final long BOTS_CHANNEL_ID = 692309264961634355L;
//    public static final long SPAM_CHANNEL_ID = 692309251418226688L;
//    public static final long CHANNELS_CHANNEL_ID = 692330710614409269L;
//    public static final long DELETED_MESSAGES_CHANNEL_ID = 734074802649825294L;
//    public static final long EDITED_MESSAGES_CHANNEL_ID = 724823562615128105L;
//
//    // Messages
//    public static final long WELCOME_MESSAGE_ID = 692310650172735530L;
//
//    // Emojis
//    public static final long CHECK_EMOJI_ID = 692324735668846653L;
//    public static final long JAM_EMOJI_ID = 0;
//    public static final long SURVIVE_EMOJI_ID = 0;
//
//    // Roles
//    public static final long ADMIN_ROLE_ID = 546161701008179200L;
//    public static final long JANITOR_ROLE_ID = 703579025733320705L;
//    public static final long TOS_ROLE_ID = 692309516435587142L;
//    public static final long ALL_ROLE_ID = 703592388047667201L;
//    public static final long NOTIFY_ROLE_ID = 707749157791596607L;

    private Server() {}

    static {
        try {
            API = JDABuilder.createDefault(Config.getToken())
                    .setBulkDeleteSplittingEnabled(false)
                    .setActivity(Activity.playing("!help"))
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.MESSAGE_CONTENT)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

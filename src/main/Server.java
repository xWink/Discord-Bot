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
    private static long generalChannel;
    private static long botsChannel;
    private static long spamChannel;
    private static long guild;

    private Server() {}

    static {
        try {
            api = new JDABuilder(AccountType.BOT).setToken(Config.getToken()).build();
            api.getPresence().setActivity(Activity.playing("!help"));
            guild = Long.parseLong(Config.getGuildId());
            generalChannel = 486633949154770946L;
            botsChannel = 551828950871965696L;
            spamChannel = 674369527731060749L;
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

    /**
     * botsChannel getter.
     *
     * @return the TextChannel where the bot is meant to be played with.
     */
    public static long getBotsChannel() {
        return botsChannel;
    }

    /**
     * spamChannel getter.
     *
     * @return the TextChannel where the bot bang-related commands are to be used.
     */
    public static long getSpamChannel() {
        return spamChannel;
    }

    /**
     * Guild getter.
     *
     * @return the guild the bot is connected to (BComp)
     */
    public static long getGuild() {
        return guild;
    }

    /**
     * Guild setter.
     *
     * @param theGuildId the ID of the guild the bot connects to (BComp)
     */
    public static void setGuild(long theGuildId) {
        guild = theGuildId;
    }

    /**
     * General Channel setter.
     *
     * @param channelId the general channel ID for the BComp Discord server
     */
    public static void setGeneralChannel(long channelId) {
        generalChannel = channelId;
    }

    /**
     * Bots Channel setter.
     *
     * @param channelId the bots channel ID for the BComp Discord server
     */
    public static void setBotsChannel(long channelId) {
        botsChannel = channelId;
    }

    /**
     * generaChannel getter.
     *
     * @return the TextChannel named general in the server.
     */
    public static long getGeneralChannel() {
        return generalChannel;
    }
}

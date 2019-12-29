package main;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 * Contains critical details about the server based on information in the config file.
 */
public final class Server {

    private static JDA api;
    private static TextChannel generalChannel;
    private static TextChannel botsChannel;
    private static Guild guild;

    private Server() {

    }

    static {
        try {
            api = new JDABuilder(AccountType.BOT).setToken(Config.getToken()).build();
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
     * @return the TextChannel where the bot is meant to be played with in.
     */
    public static TextChannel getBotsChannel() {
        return botsChannel;
    }

    /**
     * Guild getter.
     *
     * @return the guild the bot is connected to (BComp)
     */
    public static Guild getGuild() {
        return guild;
    }

    /**
     * Guild setter.
     *
     * @param theGuild the guild the bot connects to (BComp)
     */
    public static void setGuild(Guild theGuild) {
        guild = theGuild;
    }

    /**
     * General Channel setter.
     *
     * @param channel the general channel for the BComp Discord server
     */
    public static void setGeneralChannel(TextChannel channel) {
        generalChannel = channel;
    }

    /**
     * generaChannel getter.
     *
     * @return the TextChannel named general in the server.
     */
    public static TextChannel getGeneralChannel() {
        return generalChannel;
    }

    /**
     * guild getter.
     *
     * @return the Discord server (guild) the bot is running on
     */
    public static Guild guild() {
        return guild;
    }
}

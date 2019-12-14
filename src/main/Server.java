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

        //THESE DO NOT WORK

        //general channel ID = 486633949154770946
        generalChannel = api.getTextChannelById("486633949154770946");
//        generalChannel = getApi().getGuildById(Config.getGuildId())
//                .getTextChannelsByName("general", true).get(0);
        //bots channel ID = 551828950871965696
        botsChannel = api.getTextChannelById("551828950871965696");
//        botsChannel = getApi().getGuildById(Config.getGuildId())
//                .getTextChannelsByName("bots", true).get(0);
        System.out.println(api.getGuildCache().size());
        System.out.println(api.getGuilds().size());
        System.out.println(guild.getName());
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

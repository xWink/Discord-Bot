package command.commands;

import command.Command;
import main.Server;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;

public class Help extends Command {

    /**
     * Initializes the command's key to "!help".
     */
    public Help() {
        super("!help", true);
    }

    /**
     * Displays the help interface, which shows all the commands a regular
     * user has access to.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    @Override
    public void start(MessageReceivedEvent event) {
        if (event.getChannel().getIdLong() != Server.BOTS_CHANNEL_ID && event.getChannel().getIdLong() != Server.SPAM_CHANNEL_ID) {
            TextChannel bots = Server.API.getTextChannelById(Server.BOTS_CHANNEL_ID);
            if (bots != null) {
                event.getChannel().sendMessage("Say `!help` in " + bots.getAsMention() + " to see available commands!").queue();
            }
            return;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.YELLOW);
        eb.setTitle("Bot Commands:");

        eb.addField("Play Russian Roulette.",                                       "!bang", false);
        eb.addField("Shows high scores for bang.",                                  "!bangscores", false);
        eb.addField("Play blackjack against the computer.",                         "!bet !hit !stand", false);
        eb.addField("Shows when your daily reward resets.",                         "!daily", false);
        eb.addField("Flips a coin, displaying the result.",                         "!flip", false);
        eb.addField("Gifts a specified number of GryphCoins to a target user.",     "!gift [@user] [# gc]", false);
        eb.addField("Shows a user's display name based on Discord ID.",             "!id [Discord ID]", false);
        eb.addField("Shows info on a course at UoGuelph.",                          "!info [course ID]", false);
        eb.addField("View or purchase items listed on the market.",                 "!market !buy [item #]", false);
        eb.addField("Shows your latency.",                                          "!ping", false);
        eb.addField("Shows your profile.",                                          "!profile", false);

        event.getChannel().sendMessage(eb.build()).queue();
    }
}

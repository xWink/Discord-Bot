package command.commands;

import command.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

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
        eb.addField("Apply to join or create a private elective channel.",          "!join [role name]", false);
        eb.addField("Shows your upvotes, downvotes, and total karma.",              "!karma", false);
        eb.addField("View and purchase items listed on the market.",                "!market + !buy [item #]", false);
        eb.addField("Shows your bang scores.",                                      "!mybang", false);
        eb.addField("Shows your latency.",                                          "!ping", false);
        eb.addField("Shows a list of available roles to join.",                     "!roles", false);

        event.getChannel().sendMessage(eb.build()).queue();
    }
}

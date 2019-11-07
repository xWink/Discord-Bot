package command;

import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 * Abstraction of a command for the bot.
 * Every command has a key, which is the
 * input a user must provide to activate the command.
 */
abstract class Command {

    private String key;

    /**
     * Initializes the command's key.
     *
     * @param key the command's key
     */
    Command(String key) {
        this.key = key;
    }


    /**
     * Key getter.
     *
     * @return the class's key
     */
    String getKey() {
        return key;
    }


    /**
     * Every command must be able to compare a string
     * to its key to check for a match.
     *
     * @param string the user's input being compared to the key
     * @return returns true if the key matches and false otherwise
     */
    abstract boolean keyMatches(String string);


    /**
     * Every command must be able to be activated based on the event.
     */
    abstract void start(MessageReceivedEvent event);
}

package command;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 * Abstraction of a command for the bot.
 * Every command has a key, which is the
 * input a user must provide to activate the command.
 */
public abstract class Command {

    private String key;
    private boolean global;

    /**
     * Initializes the command's key.
     *
     * @param theKey the command's key
     * @param isGlobal whether the command can be used globally
     */
    public Command(String theKey, boolean isGlobal) {
        key = theKey;
        global = isGlobal;
    }

    /**
     * Key getter.
     *
     * @return the class's key
     */
    public String getKey() {
        return key;
    }

    /**
     * Every command must be able to compare a string
     * to its key to check for a match.
     *
     * @param string the user's input being compared to the key
     * @return returns true if the key matches and false otherwise
     */
    public boolean keyMatches(String string) {
        return string.equalsIgnoreCase(getKey());
    }

    /**
     * Prints the stack trace of an error and sends
     * a message to the channel that called the command
     * that experienced the exception indicating that an error
     * occurred.
     * @param event the MessageReceivedEvent that called the command
     * @param e the exception that was thrown
     */
    protected void printStackTraceAndSendMessage(MessageReceivedEvent event, Exception e) {
        e.printStackTrace();
        event.getChannel().sendMessage("An error occurred with " + getKey()
                + ". Please contact a moderator!").queue();
    }

    /**
     * Every command must be able to be activated based on the event.
     *
     * @param event the MessageReceivedEvent that triggered the command
     */
    public abstract void start(MessageReceivedEvent event);

    /**
     * Getter for whether a command is global or not. Global commands
     * can be accessed from any channel on the server while non-global
     * commands can only be accessed on specified channels in config.
     *
     * @return whether the command is global or not
     */
    public boolean isGlobal() {
        return global;
    }
}

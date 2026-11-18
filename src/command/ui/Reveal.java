package command.ui;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sends an animation with a placeholder, then swaps in the result text and buttons once the animation has played,
 * so the words never give the outcome away early. If the edit fails the result is sent as a new message.
 */
public final class Reveal {

    private Reveal() { }

    /**
     * @param action the message holding the animation, with the placeholder as its content
     * @param channel where to send the result if the edit fails
     * @param result the text that replaces the placeholder
     * @param buttons buttons to show with the result (may be empty)
     * @param delayMs how long the animation takes
     */
    public static void send(MessageCreateAction action, MessageChannel channel, String result,
                            List<ItemComponent> buttons, long delayMs) {
        action.queue(message -> {
            net.dv8tion.jda.api.requests.restaction.MessageEditAction edit = message.editMessage(result);
            if (!buttons.isEmpty()) edit.setComponents(ActionRow.of(buttons));
            edit.queueAfter(delayMs, TimeUnit.MILLISECONDS, ok -> { },
                    err -> channel.sendMessage(result).queue());
        });
    }
}

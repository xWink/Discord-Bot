package command.util.cards;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import javax.annotation.Nonnull;
import java.util.List;

public final class CardMessage {

    /**
     * Creates a MessageAction containing a given text content and the image attachment of a HandOfCards
     * if the image is valid.
     *
     * @param channel the MessageChannel to send the message in
     * @param content the text contained in the message. May not be null but may be empty.
     * @param cards the list of cards that will be combined into an image
     * @return a MessageAction ready to be sent to Discord containing a given text with an attached image of cards
     */
    public static MessageAction createCardMessage(MessageChannel channel, @Nonnull String content, List<Card> cards) {
        byte[] image;
        MessageAction message = channel.sendMessage(content);
        if ((image = PhotoCombine.genPhoto(cards)) != null)
            message.addFile(image, "out.png");
        return message;
    }
}

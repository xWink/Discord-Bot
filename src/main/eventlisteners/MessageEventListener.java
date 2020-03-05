package main.eventlisteners;

import command.Command;
import command.CommandList;
import command.util.message.MessageData;
import database.connectors.MessagesConnector;
import main.Config;
import main.Server;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class MessageEventListener extends ListenerAdapter {

    private static MessageEventListener messageEventListener;
    private MessagesConnector mc = new MessagesConnector();

    private MessageEventListener() {}

    public static MessageEventListener getMessageEventListener() {
        if (messageEventListener == null) {
            messageEventListener = new MessageEventListener();
        }
        return messageEventListener;
    }

    /**
     * Upon receiving a message, the bot checks if the message meets the
     * criteria to be a command, then compares the message to every command's
     * key. If a message matches with a command key, the command is run.
     * Messages that do not trigger bot commands are stored in the database
     *
     * @see MessageData
     * @see MessagesConnector
     * @param event the MessageReceivedEvent potentially containing a command key
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String messageContent = event.getMessage().getContentRaw();
        if (!event.getAuthor().isBot()) {
            if (messageContent.startsWith("!")) {
                for (Command command : CommandList.getCommands()) {
                    if (command.keyMatches(messageContent)
                            && (event.getChannel().getId().equals(Config.getChannels()[0]) || command.isGlobal())) {
                        command.start(event);
                        return;
                    }
                }
            }

            try {
                mc.storeMessage(event.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Searches the database for a deleted message with a matching message ID
     * and prints that message's data to an admin channel.
     *
     * @see MessageData
     * @see MessagesConnector
     * @param event the MessageDeleteEvent that triggered this handler
     */
    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        try {
            MessageData data = mc.getMessageDataById(event.getMessageIdLong());
            if (data != null) {
                TextChannel channel = Objects.requireNonNull(Server.getApi().getTextChannelById(677109914400980992L));
                if (data.getImageBase64().length() == 0) {
                    channel.sendMessage(data.toFormattedString()).queue();
                } else {
                    File file = new File("../../res/LastImage.png");
                    BufferedImage image = decodeBase64Image(data.getImageBase64());
                    if (image != null) {
                        ImageIO.write(image, "png", file);
                        channel.sendMessage(data.toFormattedString()).addFile(file).queue();
                    } else {
                        channel.sendMessage(data.toFormattedString()).queue();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageBulkDelete(@NotNull MessageBulkDeleteEvent event) {
        List<MessageData> bulkData;
        List<StringBuilder> out = new LinkedList<>();
        out.add(new StringBuilder());

        try {
            bulkData = mc.getBulkMessageDataByIds(event.getMessageIds());
        } catch (Exception e) {
            TextChannel channel = Objects.requireNonNull(Server.getApi().getTextChannelById(677109914400980992L));
            channel.sendMessage("FAILED TO ACQUIRE MESSAGES FROM PURGE").queue();
            return;
        }

        for (MessageData bulkDatum : bulkData) {
            String dataString = bulkDatum.toFormattedString();
            if (out.get(out.size() - 1).length() + dataString.length() >= 2000) { // Avoid Discord 2000 character limit
                out.add(new StringBuilder());
            }
            out.get(out.size() - 1).append(dataString); // Build output strings
        }

        for (StringBuilder sb : out) {
            Objects.requireNonNull(Server.getApi().getTextChannelById(677109914400980992L)).sendMessage(sb).queue();
        }
    }

    private BufferedImage decodeBase64Image(String string) {
        BufferedImage image = null;
        byte[] imageByte;
        try {
            imageByte = Base64.getDecoder().decode(string);
            ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
            image = ImageIO.read(bis);
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return image;
    }
}

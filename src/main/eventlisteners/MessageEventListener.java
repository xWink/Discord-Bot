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
            BufferedImage image = null;
            File file = null;

            if (data != null) {
                TextChannel channel = Objects.requireNonNull(Server.getApi().getTextChannelById(677109914400980992L));
                String part1 = data.toFormattedString();
                String part2 = "";

                if (part1.length() >= 1999) {
                    part2 = part1.substring(1999, part1.length()-1);
                    part1 = part1.substring(0, 1998);
                }

                if (data.getImageBase64().length() != 0) {
                    file = new File("../../res/LastImage.png");
                    image = decodeBase64Image(data.getImageBase64());
                }

                if (image != null) {
                    ImageIO.write(image, "png", file);
                }

                if (part2.length() > 0) {
                    sendAsTwoMessages(part1, part2, file, channel);
                } else {
                    sendAsOneMessage(part1, file, channel);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendAsOneMessage(String s, File file, TextChannel channel) {
        if (file == null) {
            channel.sendMessage(s).queue();
        } else {
            channel.sendMessage(s).addFile(file).queue();
        }
    }

    private void sendAsTwoMessages(String s1, String s2, File file, TextChannel channel) {
        if (file == null) {
            channel.sendMessage(s1).queue();
            channel.sendMessage(s2).queue();
        } else {
            channel.sendMessage(s1).queue();
            channel.sendMessage(s2).addFile(file).queue();
        }
    }

    /**
     * Searches the database for deleted messages and prints them all to an admin channel.
     * @param event the MessageBulkDeleteEvent that triggered this handler
     */
    @Override
    public void onMessageBulkDelete(@NotNull MessageBulkDeleteEvent event) {
        TextChannel channel = Objects.requireNonNull(Server.getApi().getTextChannelById(677109914400980992L));
        File file = null;
        BufferedImage image = null;
        List<MessageData> bulkData;
        StringBuilder out = new StringBuilder();

        try {
            bulkData = mc.getBulkMessageDataByIds(event.getMessageIds());

            for (MessageData data : bulkData) {
                // Avoid Discord 2000 character limit
                if (out.length() + data.toFormattedString().length() >= 1998) {
                    channel.sendMessage(out).queue();
                    out = new StringBuilder();
                }

                // Build output string
                out.append(data.toFormattedString()).append("\n");

                // Generate image
                if (data.getImageBase64().length() != 0) {
                    file = new File("../../res/LastImage.png");
                    image = decodeBase64Image(data.getImageBase64());
                }

                // Print any message with an image
                if (image != null) {
                    ImageIO.write(image, "png", file);
                    channel.sendMessage(out).addFile(file).queue();
                    image = null;
                    out = new StringBuilder();
                }
            }

            if (out.length() > 0) {
                channel.sendMessage(out).queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
            channel.sendMessage("**FAILED TO ACQUIRE MESSAGES FROM PURGE**").queue();
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

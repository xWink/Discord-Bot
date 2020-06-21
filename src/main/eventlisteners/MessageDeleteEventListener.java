package main.eventlisteners;

import command.util.misc.MessageData;
import database.connectors.MessagesConnector;
import main.Server;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MessageDeleteEventListener extends ListenerAdapter {

    private MessagesConnector mc;
    private ScheduledExecutorService scheduler;

    public MessageDeleteEventListener() {
        mc = new MessagesConnector();
        scheduler = new ScheduledThreadPoolExecutor(1);
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
        System.out.println("deleted");
        Callable<Void> storeMessage = () -> {
            try {
                MessageData data = mc.getMessageDataById(event.getMessageIdLong());
                TextChannel channel = Server.API.getTextChannelById(677109914400980992L);
                if (data != null && channel != null) {
                    String part1 = data.toFormattedString();
                    String part2 = "";

                    if (part1.length() >= 1999) {
                        part2 = part1.substring(1999, part1.length() - 1);
                        part1 = part1.substring(0, 1998);
                    }

                    File file = null;
                    BufferedImage image = null;
                    String base64Image = data.getImageBase64();
                    if (base64Image.length() != 0) {
                        file = new File("../../res/LastImage.png");
                        image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64Image)));
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
            return null;
        };
        scheduler.schedule(storeMessage, 0, TimeUnit.SECONDS);
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
}

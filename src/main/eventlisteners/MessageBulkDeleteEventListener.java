package main.eventlisteners;

import command.util.misc.MessageData;
import database.connectors.MessagesConnector;
import main.Server;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MessageBulkDeleteEventListener extends ListenerAdapter {

    private MessagesConnector mc;
    private ScheduledExecutorService scheduler;

    public MessageBulkDeleteEventListener() {
        mc = new MessagesConnector();
        scheduler = new ScheduledThreadPoolExecutor(1);
    }

    /**
     * Searches the database for deleted messages and prints them all to an admin channel.
     * @param event the MessageBulkDeleteEvent that triggered this handler
     */
    @Override
    public void onMessageBulkDelete(@NotNull MessageBulkDeleteEvent event) {
        Callable<Void> storeMessages = () -> {
            TextChannel channel = Objects.requireNonNull(Server.API.getTextChannelById(677109914400980992L));
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
                    File file = null;
                    BufferedImage image = null;
                    String base64Image = data.getImageBase64();
                    if (base64Image.length() > 0) {
                        file = new File("../../res/LastImage.png");
                        image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64Image)));
                    }

                    // Print any message with an image
                    if (image != null) {
                        ImageIO.write(image, "png", file);
                        channel.sendMessage(out).addFile(file).queue();
                        out = new StringBuilder();
                    }
                }

                if (out.length() > 0) {
                    channel.sendMessage("{BULK DELETE}\n" + out).queue();
                }
            } catch (Exception e) {
                e.printStackTrace();
                channel.sendMessage("**FAILED TO ACQUIRE MESSAGES FROM PURGE**").queue();
            }
            return null;
        };
        scheduler.schedule(storeMessages, 0, TimeUnit.SECONDS);
    }
}

package command.util.message;

import main.Server;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MessageData {

    private long messageId;
    private long authorId;
    private long channelId;
    private long timeSent;
    private String content;
    private String imageBase64;

    public MessageData() {
        this(0, 0, 0, 0, "", "");
    }

    public MessageData(long messageId, long authorId, long channelId, long timeSent, String content, String imageBase64) {
        this.messageId = messageId;
        this.authorId = authorId;
        this.channelId = channelId;
        this.timeSent = timeSent;
        this.content = content;
        this.imageBase64 = imageBase64;
    }

    public long getAuthorId() {
        return authorId;
    }

    public long getMessageId() {
        return messageId;
    }

    public long getChannelId() {
        return channelId;
    }

    public long getTimeSent() {
        return timeSent;
    }

    public String getContent() {
        return content;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public void setAuthorId(long authorId) {
        this.authorId = authorId;
    }

    public void setTimeSent(long timeSent) {
        this.timeSent = timeSent;
    }

    public void setChannelId(long channelId) {
        this.channelId = channelId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String toFormattedString() {
        Date date = new Date(getTimeSent());
        DateFormat format = new SimpleDateFormat("MMM dd yyyy, HH:mm:ss");
        String channel, author;

        TextChannel textChannel = Server.API.getTextChannelById(getChannelId());
        if (textChannel != null) {
            channel = textChannel.getName();
        } else {
            channel = "Unknown Channel";
        }

        User user = Server.API.getUserById(getAuthorId());
        if (user != null) {
            author = user.getName();
        } else {
            author = "Unknown User";
        }

        return String.format("[%s] <#%s> %s: \"%s\"", format.format(date), channel, author, getContent());
    }
}

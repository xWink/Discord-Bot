package command.util.message;

import main.Server;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class MessageData {

    private long messageId;
    private long authorId;
    private long channelId;
    private long timeSent;
    private String content;

    public MessageData() {
        this(0, 0, 0, 0, "");
    }

    public MessageData(long messageId, long authorId, long channelId, long timeSent, String content) {
        this.messageId = messageId;
        this.authorId = authorId;
        this.channelId = channelId;
        this.timeSent = timeSent;
        this.content = content;
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

    public String toFormattedString() {
        Date date = new Date(getTimeSent());
        DateFormat format = new SimpleDateFormat("MMM dd yyyy, HH:mm:ss");
        String channel = Objects.requireNonNull(Server.getApi().getTextChannelById(getChannelId())).getName();
        String author = Objects.requireNonNull(Server.getApi().getUserById(getAuthorId())).getName();
        return String.format("[%s] <#%s> %s: \"%s\"", format.format(date), channel, author, getContent());
    }
}

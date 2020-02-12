package command.util.message;

import main.Server;

import java.util.Calendar;
import java.util.Objects;

public class MessageData {

    private long messageId;
    private long authorId;
    private long timeSent;
    private String content;

    public MessageData() {
        this(0, 0, 0, "");
    }

    public MessageData(long messageId, long authorId, long timeSent, String content) {
        this.messageId = messageId;
        this.authorId = authorId;
        this.timeSent = timeSent;
        this.content = content;
    }

    public long getAuthorId() {
        return authorId;
    }

    public long getMessageId() {
        return messageId;
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

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getTimeSent());
        String authName = Objects.requireNonNull(Server.getApi().getUserById(getAuthorId())).getName();
        return String.format("[%s] %s - %s", calendar.toString(), authName, getContent());
    }
}

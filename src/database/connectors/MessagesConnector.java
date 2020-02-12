package database.connectors;

import command.util.message.MessageData;
import database.Connector;
import net.dv8tion.jda.api.entities.Message;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MessagesConnector extends Connector {

    /**
     * Initializes table to "messages".
     */
    public MessagesConnector() {
        super("messages");
    }

    public void storeMessage(Message message) throws SQLException {
        getConnection().prepareStatement("INSERT INTO " + getTable()
                + "(message_id, author_id, channel_id, time_sent, content)"
                + "VALUES (" + message.getId() + ", "
                + message.getAuthor().getId() + ", "
                + message.getChannel().getId() + ", "
                + (message.getTimeCreated().toInstant().getEpochSecond() * 1000)
                + ", '" + message.getContentRaw() + "')").executeUpdate();
    }

    public MessageData getMessageDataById(long messageId) throws SQLException {
        ResultSet rs = getConnection().prepareStatement("SELECT * FROM " + getTable()
                + " WHERE message_id = " + messageId).executeQuery();

        if (rs.first())
            return new MessageData(
                    rs.getLong("message_id"),
                    rs.getLong("author_id"),
                    rs.getLong("channel_id"),
                    rs.getLong("time_sent"),
                    rs.getString("content"));

        return null;
    }

    @Override
    protected void addUser(long userId) {
        //do nothing
    }
}

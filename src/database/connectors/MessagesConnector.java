package database.connectors;

import command.util.message.MessageData;
import database.Connector;
import net.dv8tion.jda.api.entities.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

public class MessagesConnector extends Connector {


    /**
     * Initializes table to "messages".
     */
    public MessagesConnector() {
        super("messages");
    }

    /**
     * Accepts a Message object and stores, in the database, the message ID, author's user ID,
     * channel ID of the channel that the message was sent in, the time the message was sent in (ms since epoch),
     * the string content of the message, and the base64 encoded string representing an image that
     * may be attached to the image.
     *
     * @param message the message sent in Discord
     * @throws SQLException may be thrown when accessing the database
     * @throws IOException may be thrown when handling the image downloaded from Discord
     */
    public void storeMessage(Message message) throws SQLException, IOException {
        String imageString = "";

        if (message.getAttachments().size() > 0 && message.getAttachments().get(0).isImage()) {
            File file = new File("../../res/LastImage.png");
            file = message.getAttachments().get(0).downloadToFile(file).join();
            FileInputStream inputStream = new FileInputStream(file);
            byte[] bytes = new byte[(int)file.length()];
            inputStream.read(bytes);
            imageString = new String(Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
        }

        String myStatement = "INSERT INTO " + getTable()
                + "(message_id, author_id, channel_id, time_sent, content, image_base64) "
                + "VALUES (" + message.getId() + ", "
                + message.getAuthor().getId() + ", "
                + message.getChannel().getId() + ", "
                + (message.getTimeCreated().toInstant().getEpochSecond() * 1000 - 1.8e7)
                + ", ?, ?)";
        System.out.println(myStatement);
        PreparedStatement statement = getConnection().prepareStatement(myStatement);
        statement.setString(1, message.getContentRaw());
        statement.setString(2, imageString);
        statement.executeUpdate();
    }

    /**
     * Searches the database for a message with a matching message ID.
     *
     * @param messageId the ID number of the message being searched for
     * @return the MessageData object containing all of the relevant data of the message
     *          or null if the message was not found
     * @throws SQLException may be thrown when accessing the database
     */
    public MessageData getMessageDataById(long messageId) throws SQLException {
        ResultSet rs = getConnection().prepareStatement("SELECT * FROM " + getTable()
                + " WHERE message_id = " + messageId).executeQuery();

        if (rs.first())
            return new MessageData(
                    rs.getLong("message_id"),
                    rs.getLong("author_id"),
                    rs.getLong("channel_id"),
                    rs.getLong("time_sent"),
                    rs.getString("content"),
                    rs.getString("image_base64"));

        return null;
    }

    /**
     * Non-functional method for this table.
     * @param userId ignored
     */
    @Override
    protected void addUser(long userId) {
        //do nothing
    }
}

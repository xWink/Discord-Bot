package DiscordBot.util.database;

import java.sql.Connection;

import static DiscordBot.util.database.DatabaseUtil.getConnection;

public class KarmaDB {

    private static Connection conn;

    static {
        conn = getConnection();
    }

    public static void addNewUser(long userId) {
        try {
            conn.prepareStatement("INSERT INTO karma (user, upvotes, downvotes) VALUES ("
                    + userId + ", 0, 0)").executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addUpVotes(long userId, int numUpVotes) {
        try {
            conn.prepareStatement("UPDATE karma SET upvotes = upvotes + "
                    + numUpVotes + " WHERE user = " + userId).executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addDownVotes(long userId, int numDownVotes) {
        try {
            conn.prepareStatement("UPDATE karma SET downvotes = downvotes + "
                    + numDownVotes + " WHERE user = " + userId).executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

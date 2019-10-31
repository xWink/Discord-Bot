package DiscordBot.commands.misc;

import java.sql.ResultSet;

import static DiscordBot.util.database.DatabaseUtil.getUserRowInTable;
import static DiscordBot.util.database.DatabaseUtil.userExistsInTable;
import static DiscordBot.util.database.KarmaDB.addNewUser;

public class Karma {

    private int upVotes;
    private int downVotes;
    private long userId;

    public Karma(long userId) {
        this.userId = userId;
        setUpData();
    }

    private void setUpData() {
        if (!userExistsInTable("karma", userId)) addNewUser(userId);
        try {
            ResultSet rs = getUserRowInTable("karma", userId);
            assert rs != null;
            upVotes = rs.getInt("upvotes");
            downVotes = rs.getInt("downvotes");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getKarma() {
        return upVotes - downVotes;
    }

    public int getUpVotes() {
        return upVotes;
    }

    public int getDownVotes() {
        return downVotes;
    }

    public String getSummary() {
        return "Upvotes: " + getUpVotes() + "\nDownvotes: " + getDownVotes()
                + "\nKarma: " + getKarma();
    }
}

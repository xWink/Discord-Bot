package DiscordBot.util.tictactoe_util;

import DiscordBot.util.economy.Wallet;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.Date;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class Wager {

    private long challengerId = -1;
    private long targetId = -1;
    private long creationTime = -1;
    private int wagerAmount = -1;
    private Connection conn;
    private MessageChannel channel;

    public Wager(Connection conn, MessageChannel channel){
        Date date = new Date();
        this.conn = conn;
        this.channel = channel;
        this.creationTime = date.getTime();
    }

    public long getChallengerId() {
        return this.challengerId;
    }

    public long getTargetId() {
        return this.targetId;
    }

    public int getWagerAmount() {
        return this.wagerAmount;
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public boolean setChallengerId(Message message) {
        if (verifyFormat(message)) {
            this.challengerId = message.getAuthor().getIdLong();
            return true;
        }
        return false;
    }

    public boolean setTargetId(Message message) {
        if (verifyFormat(message)) {
            if (message.getMentionedUsers().isEmpty()) {
                this.targetId = 0;
            } else {
                this.targetId = message.getMentionedUsers().get(0).getIdLong();
            }
            return true;
        }
        return false;
    }

    public boolean setWagerAmount(Message message) {
        String content = message.getContentRaw().trim();
        if (verifyFormat(message)) {
            this.wagerAmount = Integer.parseInt(content.substring(message.getContentRaw().trim().lastIndexOf(" ")+1));
            // Check if author can afford the wager
            Wallet challengerWallet = new Wallet(message.getAuthor(), this.conn);
            if (!challengerWallet.canAfford(this.wagerAmount)){
                this.channel.sendMessage("You can't afford that wager!").complete();
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean verifyFormat(Message message) {
        String content = message.getContentRaw().trim().substring(6);
        return content.matches("^ [0-9]+$")
                || content.matches("^ <@[0-9]+> [0-9]+$");
    }

    //TODO: Verify that target exists and is not author

    public void createPendingWager() {

        // Check if author already has a wager pending
        System.out.println("Made list of wagers");
        ListOfWagers wagers = new ListOfWagers();
        System.out.println("Going through list of wagers");
        for (Wager w: wagers.getWagers()) {
            if (w.getChallengerId() == this.challengerId){
                channel.sendMessage("You already have a pending wager!").complete();
                return;
            }
        }
        System.out.println("Pending wager doesn't exist, adding new wager");
        // Add new wager
        wagers.addNewWager(this);
        System.out.println("New wager added");
    }

    public void pushWager(MessageChannel channel, Connection conn) {
        try {
            PreparedStatement st = conn.prepareStatement("INSERT INTO tictactoe VALUES ("
                    + this.challengerId + ", " + this.targetId + ", " + wagerAmount
                    + "null, null, null, null, null, null, null, null, null)");
            st.executeUpdate();
        } catch (Exception e){
            e.printStackTrace();
            channel.sendMessage("Could not push your wager to database. Please contact a moderator!").queue();
        }
    }
}

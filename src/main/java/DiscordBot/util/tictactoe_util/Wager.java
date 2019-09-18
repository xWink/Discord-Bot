package DiscordBot.util.tictactoe_util;

import DiscordBot.util.economy.Wallet;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Wager {

    private long challengerId = -1;
    private long targetId = -1;
    private int wagerAmount = -1;
    private Connection conn;
    private MessageChannel channel;

    public Wager(Connection conn, MessageChannel channel){
        this.conn = conn;
        this.channel = channel;
        /*switch (setWager(channel, message, content.substring(6))) {
            case -1:
                channel.sendMessage("Invalid input!").complete();
                return;
            case 0:
                pushNewWager(channel, conn);
                channel.sendMessage("Your wager against the CPU has been set!").complete();
            case 1:
                createPendingWager(channel, message, content, conn);
                channel.sendMessage("Your wager against a player is pending acceptance!").complete();
        }*/
    }

    public long getChallengerId() {
        return challengerId;
    }

    public long getTargetId() {
        return targetId;
    }

    public int getWagerAmount() {
        return wagerAmount;
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
        for (Wager w: ListOfWagers.getWagers()) {
            if (w.getChallengerId() == this.challengerId){
                channel.sendMessage("You already have a pending wager!").complete();
                return;
            }
        }

        // Add new wager
        ListOfWagers.addNewWager(this);

        // Wait 5 minutes and prune expired wager
        try {
            Thread.sleep(300000);
            ListOfWagers.removeWager(this);
        } catch (InterruptedException e) {
            e.printStackTrace();
            channel.sendMessage("Error in pruning timer. Please contact a moderator!").complete();
        }
    }

    public void pushWager(MessageChannel channel, Connection conn) {
        try {
            PreparedStatement st = conn.prepareStatement("INSERT INTO tictactoe VALUES ("
                    + this.challengerId + ", " + this.targetId + ", " + wagerAmount + ")");
        } catch (SQLException e){
            e.printStackTrace();
            channel.sendMessage("Could not push your wager to database. Please contact a moderator!").queue();
        }
    }
}

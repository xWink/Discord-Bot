package main.timertasks;

import command.util.cards.CardMessage;
import command.util.game.BlackJackGame;
import command.util.game.BlackJackList;
import command.util.game.BlackJackStore;
import main.Server;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.TimerTask;

/**
 * Auto-stands any hand that has sat idle past the timeout so a stake is never
 * held indefinitely.
 */
public class BlackJackTimeout extends TimerTask {

    public static final long IDLE_LIMIT_MS = 2 * 60 * 1000;

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (BlackJackGame game : BlackJackList.getGames()) {
            if (now - game.getLastAction() < IDLE_LIMIT_MS) continue;
            try {
                int net = BlackJackStore.settle(game);
                TextChannel channel = Server.API.getTextChannelById(game.getChannelId());
                if (channel == null) continue;
                String result = net > 0 ? "won " + net : net < 0 ? "lost " + (-net) : "pushed";
                CardMessage.createCardMessage(channel,
                        "<@" + game.getPlayer().getUserId() + "> timed out, so the hand auto-stood and you "
                                + result + " *gc*.\nDealers hand: " + game.getDealer().getHand().toString(),
                        game.getDealer().getHand().getAsList()).queue();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

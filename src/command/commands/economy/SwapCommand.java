package command.commands.economy;

import command.Command;
import command.util.economy.Listing;
import command.util.economy.Marketplace;
import command.util.economy.RoleListing;
import economy.InsufficientFundsException;
import economy.ShopService;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * {@code !swap <colour>}: change colour for 5 GC and keep the remaining rental time.
 */
public class SwapCommand extends Command {

    public SwapCommand() {
        super("!swap", false);
    }

    @Override
    public boolean keyMatches(String s) {
        return s.toLowerCase().startsWith("!swap");
    }

    @Override
    public void start(MessageReceivedEvent event) {
        String[] a = event.getMessage().getContentRaw().split("\\s+");
        if (event.getMember() == null || a.length != 2) {
            event.getChannel().sendMessage("Say `!swap <colour>` (" + ShopService.SWAP_FEE
                    + " GC, keeps your remaining time).").queue();
            return;
        }
        Role target = null;
        for (Listing l : new Marketplace(event.getGuild()).getListings()) {
            if (l instanceof RoleListing && ((RoleListing) l).getRole().getName().equalsIgnoreCase(a[1])) {
                target = ((RoleListing) l).getRole();
            }
        }
        if (target == null) {
            event.getChannel().sendMessage("That colour is not on the market.").queue();
            return;
        }
        final Role role = target;
        String purchaseId = "swap:" + event.getMessageId();
        ShopService shop = ShopService.getInstance();
        try {
            long oldRoleId = shop.beginSwap(purchaseId, event.getAuthor().getIdLong(), role.getName(), role.getIdLong());
            if (oldRoleId == -1) {
                event.getChannel().sendMessage("You need an active colour rental to swap.").queue();
                return;
            }
            if (oldRoleId == -2) {
                event.getChannel().sendMessage("You already have that colour.").queue();
                return;
            }
            Role old = event.getGuild().getRoleById(oldRoleId);
            event.getGuild().addRoleToMember(event.getMember(), role).queue(ok -> {
                try {
                    shop.complete(purchaseId, role.getName());
                    if (old != null) event.getGuild().removeRoleFromMember(event.getMember(), old).queue();
                    event.getChannel().sendMessage("Swapped to **" + role.getName() + "**, remaining time kept.").queue();
                } catch (Exception e) {
                    e.printStackTrace();
                    event.getChannel().sendMessage("Swapped, but saving failed. Receipt `" + purchaseId + "`.").queue();
                }
            }, failure -> {
                try {
                    shop.failAndRefund(purchaseId);
                    event.getChannel().sendMessage("Discord would not swap the colour; you were refunded.").queue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (InsufficientFundsException e) {
            event.getChannel().sendMessage("A swap costs " + ShopService.SWAP_FEE + " GC.").queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

package command.commands.economy;

import command.Command;
import economy.Cosmetics;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/** {@code !equip <code>} wears an owned cosmetic; {@code !equip none} removes them all. */
public class EquipCommand extends Command {

    public EquipCommand() {
        super("!equip", true);
    }

    @Override
    public boolean keyMatches(String s) {
        return s.toLowerCase().startsWith("!equip");
    }

    @Override
    public void start(MessageReceivedEvent event) {
        String[] a = event.getMessage().getContentRaw().split("\\s+");
        if (a.length != 2) {
            event.getChannel().sendMessage("Say `!equip <code>` (see `!shop owned`) or `!equip none`. One item per slot.").queue();
            return;
        }
        try {
            long user = event.getAuthor().getIdLong();
            if (a[1].equalsIgnoreCase("none")) {
                Cosmetics.unequipAll(user);
                event.getChannel().sendMessage("Cosmetics removed.").queue();
                return;
            }
            String slot = Cosmetics.equip(user, a[1].toLowerCase());
            event.getChannel().sendMessage(slot == null ? "You do not own that, or it has expired."
                    : "Equipped in the **" + slot + "** slot. See it on `!profile`.").queue();
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }
}

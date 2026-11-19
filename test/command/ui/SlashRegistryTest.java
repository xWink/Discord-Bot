package command.ui;

import command.Command;
import main.CommandList;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import integration.DbTestBase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/** Every published slash command must have a text command behind it, or users get "not available". */
public class SlashRegistryTest extends DbTestBase {
    // Needs the database: command constructors open their connectors. Skipped without -Ddb.url.

    @Test
    public void everySlashCommandHasAHandler() {
        for (SlashCommandData data : SlashCommands.all()) {
            String text = "!" + data.getName();
            boolean handled = false;
            for (Command c : CommandList.getCommands()) {
                if (c.keyMatches(text) || c.keyMatches(text + " x") || c.keyMatches(text + " 1")) {
                    handled = true;
                    break;
                }
            }
            assertTrue("/" + data.getName() + " has no registered handler", handled);
        }
    }
}

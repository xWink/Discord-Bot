package command.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

public class HelpTest {

    @Test
    public void everyPageFitsDiscordLimits() {
        for (Help.Category c : Help.Category.values()) {
            EmbedBuilder eb = c.page();
            assertTrue(c + " too long", eb.length() <= 6000);
            assertTrue(c + " description too long", eb.getDescriptionBuilder().length() <= 4096);
            eb.build(); // throws if any limit is broken
        }
    }

    @Test
    public void everyCommandMentionedInHelpExists() throws IOException {
        Set<String> keys = new HashSet<>();
        Pattern keyPattern = Pattern.compile("super\\(\"(![a-z]+)\"");
        try (Stream<Path> files = Files.walk(Paths.get("src"))) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                Matcher m = keyPattern.matcher(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
                while (m.find()) keys.add(m.group(1));
            }
        }
        // !higher, !lower and !cashout are created with a key argument in CommandList
        keys.addAll(java.util.Arrays.asList("!higher", "!lower", "!cashout"));
        Matcher m = Pattern.compile("(![a-z]+)").matcher(source());
        while (m.find()) {
            String cmd = m.group(1);
            if (cmd.equals("!economy") || cmd.equals("!event")) continue;
            assertTrue(cmd + " is in the help text but is not a registered command", keys.contains(cmd));
        }
    }

    private static String source() throws IOException {
        String s = new String(Files.readAllBytes(Paths.get("src/command/commands/Help.java")), StandardCharsets.UTF_8);
        return s.substring(s.indexOf("OVERVIEW("), s.indexOf("final String key;"));
    }
}

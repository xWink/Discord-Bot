package database;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** A migration that is not listed in index.txt is silently never applied, so check the list is complete. */
public class MigrationIndexTest {

    @Test
    public void everyMigrationFileIsListedOnceAndInOrder() throws Exception {
        File dir = new File("res/migrations");
        List<String> listed = new ArrayList<>();
        for (String line : Files.readAllLines(new File(dir, "index.txt").toPath())) {
            if (!line.trim().isEmpty()) listed.add(line.trim());
        }
        List<String> files = new ArrayList<>();
        for (String name : dir.list()) {
            if (name.endsWith(".sql")) files.add(name);
        }
        assertEquals("duplicates in index.txt", listed.size(), new java.util.HashSet<>(listed).size());
        assertTrue("files missing from index.txt: " + files, listed.containsAll(files));
        assertTrue("index.txt names files that do not exist", files.containsAll(listed));
        List<String> byVersion = new ArrayList<>(listed);
        byVersion.sort((a, b) -> Integer.compare(version(a), version(b)));
        assertEquals("index.txt must be in version order", byVersion, listed);
    }

    private static int version(String name) {
        return Integer.parseInt(name.substring(1, name.indexOf("__")));
    }
}

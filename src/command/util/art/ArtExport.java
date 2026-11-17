package command.util.art;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes every mascot variant as a PNG so the art can be uploaded as custom server emoji
 * ({@code sh gradlew exportArt}). Output goes to {@code art-out/}: 128 px files (Discord's emoji size) and
 * 512 px files for stickers or posters.
 */
public final class ArtExport {

    private ArtExport() { }

    public static void main(String[] args) throws Exception {
        Map<String, String[]> sets = new LinkedHashMap<>();
        sets.put("mascot", new String[0]);
        sets.put("party", new String[]{"victory-fx"});
        sets.put("fireworks", new String[]{"fx-fireworks"});
        sets.put("trophy", new String[]{"fx-trophy"});
        sets.put("starstruck", new String[]{"fx-stars"});
        sets.put("sparkle", new String[]{"flair-day"});
        sets.put("royal", new String[]{"bundle-weekend"});
        sets.put("scholar", new String[]{"title-week"});
        sets.put("medal", new String[]{"badge-month"});
        sets.put("silver_medal", new String[]{"badge-set"});
        sets.put("team_flag", new String[]{"banner-club"});
        sets.put("champion", new String[]{"season-1"});
        sets.put("puzzle", new String[]{"gauntlet-1"});
        sets.put("everything", new String[]{"bundle-weekend", "fx-stars", "fx-trophy", "banner-club", "badge-month"});
        File dir = new File("art-out");
        dir.mkdirs();
        for (Map.Entry<String, String[]> e : sets.entrySet()) {
            for (int size : new int[]{128, 512}) {
                File f = new File(dir, "gryph_" + e.getKey() + "_" + size + ".png");
                ImageIO.write(Sticker.render(e.getValue().length == 0 ? Collections.<String>emptyList()
                        : Arrays.asList(e.getValue()), size, 7), "png", f);
            }
        }
        System.out.println("Wrote " + sets.size() * 2 + " files to " + dir.getAbsolutePath());
    }
}

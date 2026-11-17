package command.util.misc;

import economy.Cosmetics;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.*;

public class ProfileCardTest {

    private static Cosmetics.Loadout wearing(String team, String... codes) {
        Cosmetics.Loadout l = new Cosmetics.Loadout();
        l.codes.addAll(Arrays.asList(codes));
        l.team = team;
        return l;
    }

    private static ProfileCard.Stats stats() {
        ProfileCard.Stats s = new ProfileCard.Stats();
        s.name = "eccentriclolphus";
        s.bangs = 142;
        s.deaths = 19;
        s.jams = 4;
        s.survival = 86.6;
        s.streak = 7;
        s.karma = 12;
        s.achievements = 5;
        s.achievementsTotal = 9;
        return s;
    }

    private static BufferedImage decode(byte[] png) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(png));
    }

    @Test
    public void rendersEveryLoadoutAndSavesSamples() throws Exception {
        File dir = new File("build/profile-cards");
        dir.mkdirs();
        Object[][] cases = {
                {"default", wearing(null)},
                {"flair-title", wearing(null, "flair-day", "title-week")},
                {"bundle-fireworks", wearing(null, "bundle-weekend", "fx-fireworks", "badge-month", "badge-set")},
                {"team-confetti", wearing("Gryphon Hockey", "banner-club", "victory-fx", "gauntlet-w3")},
                {"trophy-season", wearing(null, "fx-trophy", "season-202610-arcade", "badge-month")},
                {"stars-everything", wearing("Guelph Chess Club", "bundle-weekend", "fx-stars", "banner-club",
                        "season-202610-duel", "gauntlet-w3", "badge-set", "badge-month", "flair-day")},
        };
        long previousHash = 0;
        for (Object[] c : cases) {
            byte[] png = ProfileCard.render(stats(), (Cosmetics.Loadout) c[1], 42);
            BufferedImage img = decode(png);
            assertEquals(ProfileCard.WIDTH, img.getWidth());
            assertEquals(ProfileCard.HEIGHT, img.getHeight());
            Files.write(new File(dir, c[0] + ".png").toPath(), png);
            long hash = Arrays.hashCode(png);
            assertNotEquals("each loadout should look different", previousHash, hash);
            previousHash = hash;
        }
    }

    @Test
    public void defaultCardDiffersFromDressedCard() throws Exception {
        assertFalse(Arrays.equals(ProfileCard.render(stats(), wearing(null), 1),
                ProfileCard.render(stats(), wearing(null, "bundle-weekend"), 1)));
    }

    @Test
    public void longNamesAndMissingAvatarDoNotBreak() throws Exception {
        ProfileCard.Stats s = stats();
        s.name = "AVeryLongDiscordDisplayNameThatKeepsGoingAndGoingForeverAndEver";
        assertNotNull(decode(ProfileCard.render(s, wearing("An Extremely Long Team Name Indeed Yes", "banner-club",
                "title-week"), 7)));
        s.name = "";
        assertNotNull(decode(ProfileCard.render(s, wearing(null), 7)));
    }
}

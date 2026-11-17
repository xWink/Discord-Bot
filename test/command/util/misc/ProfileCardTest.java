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
            assertEquals(ProfileCard.HEIGHT, img.getHeight()); // no inventory panel when nothing is owned
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

    @Test
    public void inventoryPanelGrowsTheCardAndMarksWornItems() throws Exception {
        long now = System.currentTimeMillis();
        java.util.List<Cosmetics.OwnedItem> owned = new java.util.ArrayList<>();
        for (String code : new String[]{"victory-fx", "fx-fireworks", "fx-trophy", "fx-stars", "flair-day", "title-week",
                "bundle-weekend", "badge-month", "badge-set", "banner-club", "event-pass", "season-1", "gauntlet-1"}) {
            owned.add(new Cosmetics.OwnedItem(code, code.equals("badge-set") ? 0 : now + 3 * 86_400_000L));
        }
        byte[] few = ProfileCard.render(stats(), wearing(null, "fx-fireworks"),
                owned.subList(0, 3), 3);
        byte[] many = ProfileCard.render(stats(), wearing(null, "fx-fireworks", "title-week"), owned, 3);
        BufferedImage fewImg = decode(few);
        BufferedImage manyImg = decode(many);
        assertTrue("an inventory adds height", fewImg.getHeight() > ProfileCard.HEIGHT);
        assertTrue("more items need more rows", manyImg.getHeight() > fewImg.getHeight());
        Files.write(new File("build/profile-cards/inventory-few.png").toPath(), few);
        Files.write(new File("build/profile-cards/inventory-many.png").toPath(), many);
    }

    @Test
    public void colourRentalAndInstallmentShowEvenWithNoCosmetics() throws Exception {
        ProfileCard.Stats s = stats();
        s.extras = java.util.Arrays.asList("Colour: orange  \u00B7  4d left", "Installment: blue  \u00B7  1200/2500 GC");
        byte[] withExtras = ProfileCard.render(s, wearing(null), java.util.Collections.<Cosmetics.OwnedItem>emptyList(), 5);
        byte[] without = ProfileCard.render(stats(), wearing(null), java.util.Collections.<Cosmetics.OwnedItem>emptyList(), 5);
        assertTrue(decode(withExtras).getHeight() > decode(without).getHeight());
        Files.write(new File("build/profile-cards/extras.png").toPath(), withExtras);
    }
}

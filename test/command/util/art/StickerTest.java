package command.util.art;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class StickerTest {

    private static void save(String name, BufferedImage img) throws Exception {
        File dir = new File("build/art");
        dir.mkdirs();
        ImageIO.write(img, "png", new File(dir, name + ".png"));
    }

    @Test
    public void rendersEveryPropAndSavesSamples() throws Exception {
        String[] codes = {"victory-fx", "fx-fireworks", "fx-trophy", "fx-stars", "flair-day", "bundle-weekend",
                "title-week", "alumni", "badge-month", "badge-set", "banner-club", "season-202610-arcade", "gauntlet-w3"};
        BufferedImage plain = Sticker.render(Collections.emptyList(), 512, 1);
        save("plain", plain);
        for (String c : codes) {
            BufferedImage img = Sticker.render(Collections.singletonList(c), 512, 1);
            assertEquals(512, img.getWidth());
            save(c, img);
            assertNotEquals(c + " should differ from the plain mascot", pixelSum(plain), pixelSum(img));
        }
        save("everything", Sticker.render(Arrays.asList("bundle-weekend", "fx-stars", "fx-trophy", "banner-club",
                "badge-month", "flair-day", "season-1"), 512, 1));
    }

    private static long pixelSum(BufferedImage img) {
        long sum = 0;
        for (int y = 0; y < img.getHeight(); y += 3) for (int x = 0; x < img.getWidth(); x += 3) sum += img.getRGB(x, y);
        return sum;
    }
}

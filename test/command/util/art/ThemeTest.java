package command.util.art;

import org.junit.After;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.awt.image.BufferedImage;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ThemeTest {

    @After
    public void reset() {
        Theme.clear();
    }

    @Test
    public void parsesBothNamesInAnyCase() {
        assertSame(Theme.LIGHT, Theme.parse(" Light "));
        assertSame(Theme.DARK, Theme.parse("DARK"));
        assertNull(Theme.parse("blue"));
    }

    @Test
    public void defaultsToDarkUntilSet() {
        assertSame(Theme.DARK, Theme.current());
        Theme.use(Theme.LIGHT);
        assertSame(Theme.LIGHT, Theme.current());
    }

    @Test
    public void eachThemeDrawsItsOwnPicture() throws Exception {
        Theme.use(Theme.DARK);
        BufferedImage dark = ImageIO.read(new ByteArrayInputStream(
                GameArt.bangCard("A", 1, GameArt.Pull.CLICK, 0, Collections.<String>emptyList(), 1)));
        Theme.use(Theme.LIGHT);
        BufferedImage light = ImageIO.read(new ByteArrayInputStream(
                GameArt.bangCard("A", 1, GameArt.Pull.CLICK, 0, Collections.<String>emptyList(), 1)));
        assertEquals(dark.getWidth(), light.getWidth());
        assertNotEquals("the card body should differ", dark.getRGB(300, 200), light.getRGB(300, 200));
    }

    @Test
    public void cachedAnimationsAreKeptPerTheme() throws Exception {
        Theme.use(Theme.DARK);
        byte[] dark = GameArt.coinSpin(true);
        Theme.use(Theme.LIGHT);
        byte[] light = GameArt.coinSpin(true);
        org.junit.Assert.assertNotSame(dark, light);
        org.junit.Assert.assertNotEquals(java.util.Arrays.hashCode(dark), java.util.Arrays.hashCode(light));
    }
}

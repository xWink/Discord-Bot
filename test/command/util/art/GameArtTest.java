package command.util.art;

import command.util.game.SlotsGame.Symbol;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GameArtTest {

    private static final long MAX_BYTES = 8L * 1024 * 1024; // Discord's upload limit is well above this

    private static void save(String name, byte[] bytes) throws Exception {
        File dir = new File("build/game-art");
        dir.mkdirs();
        Files.write(new File(dir, name).toPath(), bytes);
        assertTrue(name + " too large: " + bytes.length, bytes.length < MAX_BYTES);
    }

    private static boolean isGif(byte[] b) {
        return b.length > 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F';
    }

    @Test
    public void bangCardsDrawForEveryResult() throws Exception {
        for (GameArt.Pull pull : GameArt.Pull.values()) {
            byte[] png = GameArt.bangCard("A very long player name that should be cut off", 3, pull, 7,
                    Collections.<String>emptyList(), 1);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
            assertNotNull(img);
            assertEquals(700, img.getWidth());
            save("bang-" + pull.name().toLowerCase() + ".png", png);
        }
    }

    @Test
    public void bangAnimationSettlesOnTheCard() throws Exception {
        for (GameArt.Pull pull : GameArt.Pull.values()) {
            byte[] gif = GameArt.bangAnimation("Player", 2, pull, 3, Collections.<String>emptyList(), 1);
            assertTrue(isGif(gif));
            save("bang-anim-" + pull.name().toLowerCase() + ".gif", gif);
        }
    }

    @Test
    public void coinLandsOnEitherFace() throws Exception {
        for (boolean heads : new boolean[]{true, false}) {
            byte[] gif = GameArt.coinSpin(heads);
            assertTrue(isGif(gif));
            save(heads ? "coin-heads.gif" : "coin-tails.gif", gif);
        }
    }

    @Test
    public void diceAndSlotsAnimate() throws Exception {
        byte[] dice = GameArt.diceRoll(4, 3);
        assertTrue(isGif(dice));
        save("dice.gif", dice);
        byte[] win = GameArt.slots(new Symbol[]{Symbol.MASCOT, Symbol.MASCOT, Symbol.MASCOT}, "THREE MASCOTS x50", true);
        byte[] lose = GameArt.slots(new Symbol[]{Symbol.CHERRY, Symbol.BELL, Symbol.SEVEN}, "NO MATCH", false);
        assertTrue(isGif(win) && isGif(lose));
        save("slots-win.gif", win);
        save("slots-lose.gif", lose);
    }

    @Test
    public void roulettePicture() throws Exception {
        byte[] gif = GameArt.roulette(17);
        assertTrue(isGif(gif));
        save("roulette.gif", gif);
        assertTrue(isGif(GameArt.roulette(0)));
    }

    @Test
    public void crashPictures() throws Exception {
        save("crash-win.gif", GameArt.crash(437, 200, true));
        save("crash-lose.gif", GameArt.crash(131, 200, false));
        assertTrue(isGif(GameArt.crash(10_000_000, 100_000, true)));
    }

    @Test
    public void minesAndScratchPictures() throws Exception {
        java.util.Set<Integer> found = new java.util.HashSet<>(java.util.Arrays.asList(0, 1, 5));
        java.util.Set<Integer> mines = new java.util.HashSet<>(java.util.Arrays.asList(2, 9, 14));
        byte[] png = GameArt.minesBoard(found, mines, 9, "BOOM  -10 GC", false);
        assertEquals(420, ImageIO.read(new ByteArrayInputStream(png)).getWidth());
        save("mines-lose.png", png);
        save("mines-win.png", GameArt.minesBoard(found, mines, -1, "CASHED OUT x1.9", true));
        java.util.Random r = new java.util.Random(2);
        command.util.game.ScratchGame.Card card = command.util.game.ScratchGame.deal(r);
        for (int i = 0; i < 2000 && card.prize == null; i++) card = command.util.game.ScratchGame.deal(r);
        save("scratch-win.gif", GameArt.scratch(card, "THREE WIN", true));
    }

    @Test
    public void wordlePictures() throws Exception {
        java.util.List<String> guesses = java.util.Arrays.asList("crane", "stalk");
        save("wordle.png", GameArt.wordleBoard(guesses, "stalk", "SOLVED in 2/6"));
        byte[] gif = GameArt.wordleReveal(guesses, "stalk", "SOLVED in 2/6");
        assertTrue(isGif(gif));
        save("wordle.gif", gif);
    }
}

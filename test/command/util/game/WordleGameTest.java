package command.util.game;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WordleGameTest {

    @Test
    public void scoringMarksGreenYellowAndGrey() {
        assertArrayEquals(new int[]{2, 2, 2, 2, 2}, WordleGame.score("crane", "crane"));
        assertArrayEquals(new int[]{0, 0, 0, 0, 0}, WordleGame.score("fight", "cakes"));
        // "stalk": the a is in the word, but not in the first place
        assertArrayEquals(new int[]{1, 0, 0, 0, 0}, WordleGame.score("ahead", "stalk"));
        assertArrayEquals(new int[]{2, 2, 0, 0, 0}, WordleGame.score("stove", "stalk"));
    }

    @Test
    public void repeatedLettersAreOnlyMarkedAsOftenAsTheyAppear() {
        // "plane" has one e and it is in the last place, so only that e is marked; the others in "geese" are grey
        assertArrayEquals(new int[]{0, 0, 0, 0, 2}, WordleGame.score("geese", "plane"));
        // "hello" has two l: "llama" marks both
        assertArrayEquals(new int[]{1, 1, 0, 0, 0}, WordleGame.score("llama", "hello"));
        // "crepe" has two e: "speed" gets the green e first, then the second e is yellow
        assertArrayEquals(new int[]{0, 1, 2, 1, 0}, WordleGame.score("speed", "crepe"));
    }

    @Test
    public void guessesNeedFiveLowercaseLetters() {
        assertTrue(WordleGame.validGuess("crane"));
        assertFalse(WordleGame.validGuess("cranes"));
        assertFalse(WordleGame.validGuess("Crane"));
        assertFalse(WordleGame.validGuess("cr4ne"));
        assertFalse(WordleGame.validGuess(null));
    }

    @Test
    public void everyDayHasAFiveLetterWordAndItRepeatsOnlyAfterTheList() {
        assertEquals(WordleGame.wordFor(20000), WordleGame.wordFor(20000));
        assertEquals(5, WordleGame.wordFor(-5).length());
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int d = 0; d < 600; d++) seen.add(WordleGame.wordFor(d));
        assertEquals("no repeats within 600 days", 600, seen.size());
    }
}

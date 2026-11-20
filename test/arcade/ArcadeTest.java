package arcade;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ArcadeTest {

    @Test
    public void fingerprintIgnoresChoiceOrderCaseAndSpacing() {
        String a = QuestionBank.fingerprint("What  is X?", Arrays.asList("One", "two", "Three"));
        String b = QuestionBank.fingerprint("what is x?", Arrays.asList("three", "ONE", "two"));
        assertEquals(a, b);
    }

    @Test
    public void fingerprintDiffersForDifferentQuestions() {
        assertNotEquals(QuestionBank.fingerprint("A?", Arrays.asList("1", "2")),
                QuestionBank.fingerprint("B?", Arrays.asList("1", "2")));
    }

    @Test
    public void weeksStartOnMonday() {
        // Epoch day 4 is Monday 1970-01-05, epoch day 10 is the following Sunday.
        assertEquals(PunchCard.weekOf(4), PunchCard.weekOf(10));
        assertNotEquals(PunchCard.weekOf(4), PunchCard.weekOf(11));
        assertNotEquals(PunchCard.weekOf(3), PunchCard.weekOf(4));
    }

    @Test
    public void parsesTheTriviaApiPayload() {
        String json = "[{\"category\":\"science\",\"id\":\"x\",\"correctAnswer\":\"Au\","
                + "\"incorrectAnswers\":[\"Ag\",\"Fe\",\"Pb\"],\"question\":{\"text\":\"Symbol for gold?\"},"
                + "\"difficulty\":\"easy\"}]";
        java.util.List<QuestionSources.Parsed> list = QuestionSources.parseTheTriviaApi(json, new java.util.Random(1));
        assertEquals(1, list.size());
        assertEquals("Symbol for gold?", list.get(0).prompt);
        assertEquals("Au", list.get(0).choices.get(list.get(0).correct));
        assertEquals(4, list.get(0).choices.size());
    }

    @Test
    public void packParserSkipsInvalidEntries() {
        String json = "[{\"kind\":\"code\",\"prompt\":\"p\",\"choices\":[\"a\",\"b\"],\"answer\":1},"
                + "{\"kind\":\"code\",\"prompt\":\"bad answer\",\"choices\":[\"a\",\"b\"],\"answer\":5},"
                + "{\"kind\":\"code\",\"prompt\":\"one choice\",\"choices\":[\"a\"],\"answer\":0}]";
        assertEquals(1, QuestionSources.parsePack(json).size());
    }
}

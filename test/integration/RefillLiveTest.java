package integration;

import arcade.QuestionBank;
import arcade.QuestionRefiller;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/** Hits the real Open Trivia DB. Run with -Dlive.net=true; skipped otherwise. */
public class RefillLiveTest extends DbTestBase {

    @Test
    public void openTriviaDbRefillStoresQuestions() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("live.net"));
        exec("DELETE FROM question_refills");
        int before = QuestionBank.stock("trivia", null);
        QuestionRefiller.requestRefill(0, null);
        for (int i = 0; i < 40 && QuestionBank.stock("trivia", null) == before; i++) Thread.sleep(1000);
        assertTrue(QuestionBank.stock("trivia", null) > before);
    }
}

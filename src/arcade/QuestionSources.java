package arcade;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure parsers for question providers, kept separate from networking so they
 * can be tested with sample payloads.
 */
public final class QuestionSources {

    private QuestionSources() { }

    /** A question ready to be stored. */
    public static final class Parsed {
        public final String kind;
        public final String category;
        public final String difficulty;
        public final String prompt;
        public final List<String> choices;
        public final int correct;
        public final String explanation;

        Parsed(String kind, String category, String difficulty, String prompt, List<String> choices, int correct,
               String explanation) {
            this.kind = kind;
            this.category = category;
            this.difficulty = difficulty;
            this.prompt = prompt;
            this.choices = choices;
            this.correct = correct;
            this.explanation = explanation;
        }
    }

    /**
     * The Trivia API v2: an array of questions with {@code correctAnswer},
     * {@code incorrectAnswers} and {@code question.text}. Its free data is
     * CC BY-NC 4.0, so callers must only enable it for noncommercial use.
     */
    public static List<Parsed> parseTheTriviaApi(String json, Random random) {
        List<Parsed> out = new ArrayList<>();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject q = array.getJSONObject(i);
            String correct = q.getString("correctAnswer");
            List<String> choices = new ArrayList<>();
            choices.add(correct);
            JSONArray wrong = q.getJSONArray("incorrectAnswers");
            for (int j = 0; j < wrong.length(); j++) choices.add(wrong.getString(j));
            Collections.shuffle(choices, random);
            out.add(new Parsed("trivia", q.optString("category", "General"), q.optString("difficulty", "medium"),
                    q.getJSONObject("question").getString("text"), choices, choices.indexOf(correct), null));
        }
        return out;
    }

    /**
     * A curated puzzle pack file: the same schema as the bundled pool. Answers
     * must be pre-verified by the pack's maintainers. Invalid entries are skipped.
     */
    public static List<Parsed> parsePack(String json) {
        List<Parsed> out = new ArrayList<>();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            JSONArray c = o.optJSONArray("choices");
            int answer = o.optInt("answer", -1);
            String kind = o.optString("kind", "");
            if (c == null || c.length() < 2 || answer < 0 || answer >= c.length() || kind.isEmpty()
                    || o.optString("prompt", "").isEmpty()) continue;
            List<String> choices = new ArrayList<>();
            for (int j = 0; j < c.length(); j++) choices.add(c.getString(j));
            out.add(new Parsed(kind, o.optString("category", "Code"), o.optString("difficulty", "easy"),
                    o.getString("prompt"), choices, answer, o.optString("explanation", null)));
        }
        return out;
    }
}

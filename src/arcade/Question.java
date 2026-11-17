package arcade;

import java.util.List;

/**
 * A quiz or code-reading question. The answer key stays server-side until the
 * round ends.
 */
public final class Question {

    private final long id;
    private final String kind;
    private final String category;
    private final String difficulty;
    private final String prompt;
    private final List<String> choices;
    private final int correctIndex;
    private final String explanation;
    private final String license;
    private final String attribution;

    public Question(long id, String kind, String category, String difficulty, String prompt, List<String> choices,
                    int correctIndex, String explanation, String license, String attribution) {
        this.id = id;
        this.kind = kind;
        this.category = category;
        this.difficulty = difficulty;
        this.prompt = prompt;
        this.choices = choices;
        this.correctIndex = correctIndex;
        this.explanation = explanation;
        this.license = license;
        this.attribution = attribution;
    }

    public long getId() { return id; }
    public String getKind() { return kind; }
    public String getCategory() { return category; }
    public String getDifficulty() { return difficulty; }
    public String getPrompt() { return prompt; }
    public List<String> getChoices() { return choices; }
    public int getCorrectIndex() { return correctIndex; }
    public String getExplanation() { return explanation; }
    public String getLicense() { return license; }
    public String getAttribution() { return attribution; }
}

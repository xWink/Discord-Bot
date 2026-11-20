package command.util.game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** The daily five-letter word game: the word for each day, and how a guess scores against it. */
public final class WordleGame {

    public static final int MAX_GUESSES = 6;
    public static final int LENGTH = 5;
    public static final int REWARD = 3;

    public static final int GREY = 0;
    public static final int YELLOW = 1;
    public static final int GREEN = 2;

    private static List<String> order;

    private WordleGame() { }

    private static synchronized List<String> order() {
        if (order == null) {
            List<String> words = new ArrayList<>();
            try (InputStream in = WordleGame.class.getClassLoader().getResourceAsStream("wordle/answers.txt")) {
                if (in == null) throw new IllegalStateException("wordle/answers.txt is missing");
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim().toLowerCase();
                    if (line.length() == LENGTH) words.add(line);
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            // a fixed shuffle, so the order looks random but every day (and every restart) agrees on the word
            Collections.shuffle(words, new Random(20240229L));
            order = words;
        }
        return order;
    }

    /** @param epochDay days since 1970-01-01 */
    public static String wordFor(long epochDay) {
        List<String> o = order();
        return o.get((int) Math.floorMod(epochDay, (long) o.size()));
    }

    /** Any five letters are accepted: there is no dictionary to check against. */
    public static boolean validGuess(String guess) {
        if (guess == null || guess.length() != LENGTH) return false;
        for (char c : guess.toCharArray()) if (c < 'a' || c > 'z') return false;
        return true;
    }

    /**
     * @return one score per letter: {@link #GREEN} right place, {@link #YELLOW} in the word elsewhere, {@link #GREY}
     *         not in the word (repeated letters are only marked as many times as they appear)
     */
    public static int[] score(String guess, String answer) {
        int[] out = new int[LENGTH];
        int[] left = new int[26];
        for (int i = 0; i < LENGTH; i++) {
            if (guess.charAt(i) == answer.charAt(i)) out[i] = GREEN;
            else left[answer.charAt(i) - 'a']++;
        }
        for (int i = 0; i < LENGTH; i++) {
            if (out[i] == GREEN) continue;
            int c = guess.charAt(i) - 'a';
            if (left[c] > 0) {
                out[i] = YELLOW;
                left[c]--;
            }
        }
        return out;
    }
}

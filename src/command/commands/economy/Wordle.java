package command.commands.economy;

import command.Command;
import command.ui.Reveal;
import command.util.art.GameArt;
import command.util.game.WordleGame;
import economy.WordleStore;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 * The daily word. {@code !wordle <word>} makes a guess (six per day, the same word for everyone); {@code !wordle}
 * shows your board. Green is right, yellow is in the word elsewhere, grey is not in it. Solving it pays 3 GC and
 * builds a streak. Any five letters count as a guess: there is no dictionary.
 */
public class Wordle extends Command {

    public Wordle() {
        super("!wordle", true);
    }

    @Override
    public boolean keyMatches(String string) {
        return string.toLowerCase().startsWith(getKey());
    }

    @Override
    public void start(MessageReceivedEvent event) {
        long user = event.getAuthor().getIdLong();
        String[] args = event.getMessage().getContentRaw().trim().split("\\s+");
        long day = LocalDate.now().toEpochDay();
        String answer = WordleGame.wordFor(day);
        try {
            WordleStore.Board board = WordleStore.load(user, day);
            if (args.length == 1) {
                if (board.guesses.isEmpty()) {
                    event.getChannel().sendMessage("Guess today's five-letter word in six tries: `!wordle <word>`. Green is "
                            + "the right place, yellow is in the word elsewhere, grey is not in it. Solving it pays "
                            + WordleGame.REWARD + " GC and builds a streak. Everyone gets the same word.").queue();
                    return;
                }
                sendBoard(event, board, answer, false, user, day);
                return;
            }
            String guess = args[1].toLowerCase();
            if (args.length > 2 || !WordleGame.validGuess(guess)) {
                event.getChannel().sendMessage("A guess is five letters: `!wordle <word>`.").queue();
                return;
            }
            if (board.over()) {
                sendBoard(event, board, answer, false, user, day);
                return;
            }
            WordleStore.Board after = WordleStore.guess(user, day, guess, answer);
            if (after == null) { // finished by another message in the meantime
                sendBoard(event, WordleStore.load(user, day), answer, false, user, day);
                return;
            }
            sendBoard(event, after, answer, true, user, day);
        } catch (Exception e) {
            printStackTraceAndSendMessage(event, e);
        }
    }

    private void sendBoard(MessageReceivedEvent event, WordleStore.Board board, String answer, boolean animate, long user,
                           long day) throws Exception {
        String footer = board.solved ? "SOLVED in " + board.guesses.size() + "/" + WordleGame.MAX_GUESSES
                : board.over() ? "The word was " + answer.toUpperCase()
                : board.guesses.size() + "/" + WordleGame.MAX_GUESSES + " guesses";
        String name = economy.Cosmetics.nameWithTitle(user, event.getAuthor().getName());
        String text;
        if (board.solved) {
            text = (board.paid > 0 ? economy.Cosmetics.winHeader(user) : "") + name + " solved it in " + board.guesses.size()
                    + "! " + (board.paid > 0 ? "+" + board.paid + " GC. " : "") + "Streak: "
                    + WordleStore.streak(user, day) + (WordleStore.streak(user, day) == 1 ? " day." : " days.");
        } else if (board.over()) {
            text = name + " ran out of guesses. The word was **" + answer.toUpperCase() + "**. Come back tomorrow.";
        } else {
            text = name + ": " + (6 - board.guesses.size()) + " guesses left.";
        }
        if (animate) {
            Reveal.send(event.getChannel().sendFiles(FileUpload.fromData(
                            GameArt.wordleReveal(board.guesses, answer, footer), "wordle.gif"))
                            .setContent("Checking…"), event.getChannel(), text, new ArrayList<>(), 1800);
        } else {
            event.getChannel().sendFiles(FileUpload.fromData(GameArt.wordleBoard(board.guesses, answer, footer), "wordle.png"))
                    .setContent(text).queue();
        }
    }
}

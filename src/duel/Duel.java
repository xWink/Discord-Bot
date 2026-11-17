package duel;

import arcade.Question;

/**
 * One head-to-head round on a shared question.
 */
public final class Duel {

    public static final long ACCEPT_WINDOW_MS = 2 * 60 * 1000;
    public static final long ANSWER_WINDOW_MS = (arcade.ArcadeService.ROUND_SECONDS + 5) * 1000;

    public final long id;
    public final long challenger;
    public final long opponent;
    public final int stake;
    public final long channelId;
    public final long createdAt = System.currentTimeMillis();

    Question question;
    long startedAt;
    int challengerChoice = -1;
    int opponentChoice = -1;
    long challengerTime;
    long opponentTime;

    public Duel(long id, long challenger, long opponent, int stake, long channelId) {
        this.id = id;
        this.challenger = challenger;
        this.opponent = opponent;
        this.stake = stake;
        this.channelId = channelId;
    }

    public Question getQuestion() { return question; }
    public boolean isActive() { return question != null; }

    public String reservationFor(long user) {
        return "duel:" + id + ":" + user;
    }

    /** Records an answer; later answers from the same player are ignored. */
    synchronized boolean submit(long user, int choice, long now) {
        if (user == challenger && challengerChoice < 0) {
            challengerChoice = choice;
            challengerTime = now - startedAt;
            return true;
        }
        if (user == opponent && opponentChoice < 0) {
            opponentChoice = choice;
            opponentTime = now - startedAt;
            return true;
        }
        return false;
    }

    boolean bothAnswered() {
        return challengerChoice >= 0 && opponentChoice >= 0;
    }

    private boolean right(int choice, long time) {
        return choice >= 0 && time <= ANSWER_WINDOW_MS && choice == question.getCorrectIndex();
    }

    /**
     * @return the winner's ID, or 0 for a draw (both wrong, or an exact tie on time)
     */
    long winner() {
        boolean a = right(challengerChoice, challengerTime);
        boolean b = right(opponentChoice, opponentTime);
        if (a && !b) return challenger;
        if (b && !a) return opponent;
        if (a && challengerTime != opponentTime) return challengerTime < opponentTime ? challenger : opponent;
        return 0;
    }
}

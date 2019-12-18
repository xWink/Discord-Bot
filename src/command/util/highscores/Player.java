package command.util.highscores;

public abstract class Player {

    private long id;

    public Player(long userId) {
        id = userId;
    }

    public long getId() {
        return id;
    }
}

package command.util.highscores;

public abstract class Player {

    protected long id;

    public Player(long userId) {
        id = userId;
    }

    public long getId() {
        return id;
    }
}

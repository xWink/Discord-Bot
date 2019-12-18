package command.util.highscores;


import main.Server;

public abstract class HighScore {

    protected Guild guild; // todo: import

    HighScore() {
        guild = Server.getGuild();
    }

    @Override
    public abstract String toString();
}

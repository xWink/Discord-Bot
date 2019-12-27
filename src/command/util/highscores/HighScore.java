package command.util.highscores;


import main.Server;
import net.dv8tion.jda.core.entities.Guild;

public abstract class HighScore {

    protected Guild guild; // todo: import

    HighScore() {
        guild = Server.getGuild();
    }

    @Override
    public abstract String toString();
}

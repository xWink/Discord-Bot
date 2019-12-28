package command.util.highscores;


import main.Server;
import net.dv8tion.jda.core.entities.Guild;

public abstract class HighScore {

    protected Guild guild;

    protected HighScore() {
        guild = Server.getGuild();
    }

    public abstract void update();

    @Override
    public abstract String toString();
}

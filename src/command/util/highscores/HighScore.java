package command.util.highscores;


import main.Server;
import net.dv8tion.jda.core.entities.Guild;

public abstract class HighScore {

    private Guild guild;

    protected HighScore() {
        guild = Server.getGuild();
    }

    /**
     * High scores must be able to be updated to ensure
     * currency.
     */
    public abstract void update();

    /**
     * Getter for the guild which the high score list is for.
     *
     * @return the guild for this HighScore object
     */
    protected Guild getGuild() {
        return guild;
    }

    @Override
    public abstract String toString();
}

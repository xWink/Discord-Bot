package DiscordBot.util.economy;

import net.dv8tion.jda.core.entities.Role;

public class Listing {

    private Role role;
    private int cost;

    public Listing(Role role, int cost){
        this.role = role;
        this.cost = cost;
    }

    public Role getRole() {
        return role;
    }

    public int getCost() {
        return cost;
    }
}

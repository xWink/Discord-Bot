package command.util.economy;

import main.Server;
import net.dv8tion.jda.core.entities.Role;

import java.util.ArrayList;

public final class Marketplace {

    private static ArrayList<Listing> listings;

    private Marketplace() {

    }

    static {
        listings = new ArrayList<>();

        Role orange = Server.getGuild().getRolesByName("orange", true).get(0);
        Role blue = Server.getGuild().getRolesByName("blue", true).get(0);
        Role green = Server.getGuild().getRolesByName("green", true).get(0);
        Role purple = Server.getGuild().getRolesByName("purple", true).get(0);
        Role pink = Server.getGuild().getRolesByName("pink", true).get(0);

        // Lasts 1 week
        listings.add(new RoleListing(75, 7, orange));
        listings.add(new RoleListing(75, 7, blue));
        listings.add(new RoleListing(75, 7, green));
        listings.add(new RoleListing(75, 7, purple));
        listings.add(new RoleListing(75, 7, pink));

        // Lasts 100 years
        listings.add(new RoleListing(2500, 36500, orange));
        listings.add(new RoleListing(2500, 36500, blue));
        listings.add(new RoleListing(2500, 36500, green));
        listings.add(new RoleListing(2500, 36500, purple));
        listings.add(new RoleListing(2500, 36500, pink));
    }

    /**
     * Listings getter.
     *
     * @return an ArrayList with all listings in the marketplace
     */
    public static ArrayList<Listing> getListings() {
        return listings;
    }

    /**
     * Listing getter.
     *
     * @param i the index of the listing in the marketplace
     * @return the listing at the specified index in the marketplace
     */
    public static Listing getListing(int i) {
        return listings.get(i);
    }
}

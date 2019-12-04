package command.util.economy;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

import java.util.ArrayList;

public final class Marketplace {

    private static ArrayList<Listing> listings;
    private static Guild guild;

    /**
     * Initializes a new marketplace for a specific guild.
     *
     * @param theGuild the guild for which a marketplace is created
     */
    public Marketplace(Guild theGuild) {
        listings = new ArrayList<>();
        guild = theGuild;
        setListings();
    }

    private void setListings() {
        Role orange = guild.getRolesByName("orange", true).get(0);
        Role blue = guild.getRolesByName("blue", true).get(0);
        Role green = guild.getRolesByName("green", true).get(0);
        Role purple = guild.getRolesByName("purple", true).get(0);
        Role pink = guild.getRolesByName("pink", true).get(0);

        // Lasts 1 week
        listings.add(new RoleListing(35, 7, orange));
        listings.add(new RoleListing(35, 7, blue));
        listings.add(new RoleListing(35, 7, green));
        listings.add(new RoleListing(35, 7, purple));
        listings.add(new RoleListing(35, 7, pink));

        // Lasts 100 years
        listings.add(new RoleListing(2500, 99999, orange));
        listings.add(new RoleListing(2500, 99999, blue));
        listings.add(new RoleListing(2500, 99999, green));
        listings.add(new RoleListing(2500, 99999, purple));
        listings.add(new RoleListing(2500, 99999, pink));
    }

    /**
     * Listings getter.
     *
     * @return an ArrayList with all listings in the marketplace
     */
    public ArrayList<Listing> getListings() {
        return listings;
    }

    /**
     * Listing getter.
     *
     * @param i the index of the listing in the marketplace
     * @return the listing at the specified index in the marketplace
     */
    public Listing getListing(int i) {
        return listings.get(i);
    }
}

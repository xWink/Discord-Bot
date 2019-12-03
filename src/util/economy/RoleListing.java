package util.economy;

import net.dv8tion.jda.core.entities.Role;

public class RoleListing extends Listing {

    /**
     * The role that is being listed on the marketplace.
     */
    private Role role;

    /**
     * Initializes cost and duration as well as the
     * role that is being sold.
     *
     * @param theCost the role's cost
     * @param theDuration how long the role lasts
     * @param theRole the role that is being sold
     */
    public RoleListing(int theCost, int theDuration, Role theRole) {
        super(theCost, theDuration);
        role = theRole;
    }

    /**
     * Returns the name of the listed role as the
     * name of the listing.
     *
     * @return the listed role's name
     */
    @Override
    public String toString() {
        return role.getName() + " (" + getDuration() + ") - " + getCost() + " *gc*";
    }

    /**
     * Role getter.
     *
     * @return the role listed on the marketplace
     */
    public Role getRole() {
        return role;
    }
}

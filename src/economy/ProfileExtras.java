package economy;

import database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Non-cosmetic things worth showing on the profile card: the colour role being rented and any permanent colour
 * installment in progress.
 */
public final class ProfileExtras {

    private static final long TEN_YEARS_MS = 10L * 365 * 24 * 60 * 60 * 1000;

    private ProfileExtras() { }

    /** @return short labels such as "Colour: orange · 4d left" or "Installment: blue 1200/2500 GC" */
    public static List<String> pills(long user) {
        List<String> list = new ArrayList<>();
        try {
            Database.inTransaction(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT role_colour, role_expiry FROM economy WHERE user = ?")) {
                    ps.setLong(1, user);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getString(1) != null && rs.getLong(2) > System.currentTimeMillis()) {
                            list.add("Colour: " + rs.getString(1) + "  ·  " + timeLeft(rs.getLong(2)));
                        }
                    }
                }
                return null;
            });
            InstallmentService.Plan plan = InstallmentService.getInstance().current(user);
            if (plan != null) {
                list.add("Installment: " + plan.colour + "  ·  " + plan.paid + "/" + plan.target + " GC");
            }
        } catch (SQLException e) {
            e.printStackTrace(); // extras are optional
        }
        return list;
    }

    static String timeLeft(long expiresAt) {
        long ms = expiresAt - System.currentTimeMillis();
        if (ms > TEN_YEARS_MS) return "permanent";
        long days = ms / 86_400_000L;
        if (days >= 1) return days + "d left";
        long hours = ms / 3_600_000L;
        return hours >= 1 ? hours + "h left" : "<1h left";
    }
}

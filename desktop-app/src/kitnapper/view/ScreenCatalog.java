package kitnapper.view;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kitnapper.model.UserRole;

/**
 * Screen metadata for the shell (design.md §5): the title and subtitle each
 * route shows, plus the role-based navigation order and default screen.
 * {@link MainFrame} owns layout; this class owns the routing vocabulary.
 */
final class ScreenCatalog {

    /** Title and one-line subtitle for a screen route. */
    record Meta(String title, String subtitle) { }

    private static final Map<String, Meta> SCREENS = new LinkedHashMap<>();

    static {
        put("catalog", "Catalog", "Browse the lending catalog.");
        put("myloans", "My Loans", "Your active and past checkouts.");
        put("dashboard", "Dashboard", "Overdue loans and unpaid penalties at a glance.");
        put("allloans", "All Loans", "Every loan across all borrowers.");
        put("overdue", "Overdue", "Loans past their due date, longest first.");
        put("penalties", "Penalties", "Late fees waiting to be settled.");
        put("inventory", "Inventory", "The full equipment inventory.");
        put("settings", "Settings / Data", "Theme, save file, and demo data.");
    }

    private ScreenCatalog() {
    }

    private static void put(String key, String title, String subtitle) {
        SCREENS.put(key, new Meta(title, subtitle));
    }

    /** Metadata for a route, or null when the key is unknown. */
    static Meta meta(String key) {
        return SCREENS.get(key);
    }

    /** Every known route, in declaration order. */
    static List<String> keys() {
        return List.copyOf(SCREENS.keySet());
    }

    /** Route key to title, for the sidebar. */
    static Map<String, String> titles() {
        Map<String, String> titles = new LinkedHashMap<>();
        for (Map.Entry<String, Meta> entry : SCREENS.entrySet()) {
            titles.put(entry.getKey(), entry.getValue().title());
        }
        return titles;
    }

    static String defaultKey(UserRole role) {
        return role == UserRole.MANAGER ? "dashboard" : "catalog";
    }

    static List<String> navKeys(UserRole role) {
        return role == UserRole.MANAGER
            ? List.of("dashboard", "allloans", "overdue", "penalties", "inventory")
            : List.of("catalog", "myloans");
    }
}

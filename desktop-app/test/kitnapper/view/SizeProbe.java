package kitnapper.view;

import kitnapper.model.UserRole;

/**
 * Chrome metrics probe (plan §6.4, §9.4): the 32/48/240/32 chrome heights
 * and the role-based sidebar button counts (manager 6 including Settings).
 * Kept thin by design (WP0.5 note): WP3 rewrites the theme surface and this
 * is one of its two guards. Runs headless.
 */
public final class SizeProbe {

    private static int passed;
    private static int failed;

    private SizeProbe() { }

    public static void main(String[] args) {
        check("chrome-heights",
            new TitleBar(null, null).getPreferredSize().height == 32
            && new Omnibar().getPreferredSize().height == 48
            && new StatusBar().getPreferredSize().height == 32);

        Sidebar manager = new Sidebar(UserRole.MANAGER, "A. Manager",
            ScreenCatalog.titles(), ScreenCatalog.navKeys(UserRole.MANAGER),
            stubCallbacks());
        Sidebar borrower = new Sidebar(UserRole.BORROWER, "Student One",
            ScreenCatalog.titles(), ScreenCatalog.navKeys(UserRole.BORROWER),
            stubCallbacks());
        check("sidebar-buttons", manager.getPreferredSize().width == 240
            && countNavButtons(manager) == 6
            && countNavButtons(borrower) == 2);

        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** Nav entries the sidebar actually created, counted over all route keys. */
    private static int countNavButtons(Sidebar sidebar) {
        int count = 0;
        for (String key : ScreenCatalog.keys()) {
            if (sidebar.defaultButton(key) != null) {
                count++;
            }
        }
        return count;
    }

    private static Sidebar.Callbacks stubCallbacks() {
        return new Sidebar.Callbacks() {
            public void navigate(String key) { /* probe stub */ }

            public void signOut() { /* probe stub */ }
        };
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}

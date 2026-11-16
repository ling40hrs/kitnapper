package kitnapper.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import kitnapper.model.UserRole;
import kitnapper.service.AppContext;
import kitnapper.view.AllLoansScreen;
import kitnapper.view.CatalogScreen;
import kitnapper.view.DashboardScreen;
import kitnapper.view.MainFrame;
import kitnapper.view.MyLoansScreen;
import kitnapper.view.OverdueScreen;
import kitnapper.view.PenaltiesScreen;

/**
 * Screen construction and mounting for one signed-in session (plan §6.5).
 * Extracted from {@link AppController} so the session shell keeps login,
 * routing and the theme toggle while this class owns which screens exist for
 * a role, how they mount, and the theme-refresh list. One instance per
 * session; {@link AppController} drops it on sign-out.
 */
final class ScreenMounts {

    private final AppContext context;
    private final MainFrame frame;
    private final BiConsumer<String, String> status;
    private final List<Runnable> refreshers = new ArrayList<>();
    private CatalogController catalog;
    private LoansController loans;
    private PenaltyController penalties;
    private DashboardController dashboard;
    private CatalogScreen catalogScreen;
    private MyLoansScreen myLoansScreen;
    private AllLoansScreen allLoansScreen;
    private OverdueScreen overdueScreen;
    private PenaltiesScreen penaltiesScreen;
    private DashboardScreen dashboardScreen;

    ScreenMounts(AppContext context, MainFrame frame, BiConsumer<String, String> status) {
        this.context = context;
        this.frame = frame;
        this.status = status;
    }

    /** Mounts the catalog every role gets, then the role's own screens. */
    void mount(UserRole role) {
        catalog = new CatalogController(context);
        loans = new LoansController(context);
        penalties = new PenaltyController(context);
        catalogScreen = new CatalogScreen(
            new CatalogActions(catalog, new BorrowingController(context), status),
            frame.searchField());
        catalog.setOnChanged(catalogScreen::reload);
        frame.mount("catalog", catalogScreen, catalogScreen.commands(),
            catalogScreen::onShown);
        refreshers.add(catalogScreen::refreshTheme);
        if (role == UserRole.MANAGER) {
            mountManager();
        } else {
            mountBorrower();
        }
    }

    /** Dashboard, All Loans, Overdue and Penalties (§8.3–§8.7). */
    private void mountManager() {
        LoansActions actions = new LoansActions(loans);
        dashboard = new DashboardController(context);
        dashboardScreen = new DashboardScreen(dashboard, status);
        allLoansScreen = new AllLoansScreen(actions, status);
        overdueScreen = new OverdueScreen(actions, status);
        penaltiesScreen = new PenaltiesScreen(penalties, status);
        frame.mount("dashboard", dashboardScreen, dashboardScreen.commands(),
            dashboardScreen::onShown);
        frame.mount("allloans", allLoansScreen, allLoansScreen.commands(),
            allLoansScreen::onShown);
        frame.mount("overdue", overdueScreen, overdueScreen.commands(),
            overdueScreen::onShown);
        frame.mount("penalties", penaltiesScreen, penaltiesScreen.commands(),
            penaltiesScreen::onShown);
        dashboard.setOnChanged(dashboardScreen::reload);
        loans.setOnChanged(this::reloadLoanScreens);
        penalties.setOnChanged(penaltiesScreen::reload);
        refreshers.add(dashboardScreen::refreshTheme);
        refreshers.add(allLoansScreen::refreshTheme);
        refreshers.add(overdueScreen::refreshTheme);
        refreshers.add(penaltiesScreen::refreshTheme);
    }

    /** My Loans (§8.3). */
    private void mountBorrower() {
        myLoansScreen = new MyLoansScreen(new LoansActions(loans), status);
        loans.setOnChanged(myLoansScreen::reload);
        frame.mount("myloans", myLoansScreen, myLoansScreen.commands(),
            myLoansScreen::onShown);
        refreshers.add(myLoansScreen::refreshTheme);
    }

    /** One app-level change can move rows between All Loans and Overdue. */
    private void reloadLoanScreens() {
        if (allLoansScreen != null) {
            allLoansScreen.reload();
        }
        if (overdueScreen != null) {
            overdueScreen.reload();
        }
    }

    /** Runs every screen's token refresh after a Theme switch. */
    void refreshTheme() {
        for (Runnable refresh : refreshers) {
            refresh.run();
        }
    }
}

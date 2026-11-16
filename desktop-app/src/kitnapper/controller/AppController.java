package kitnapper.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import kitnapper.model.User;
import kitnapper.model.UserRole;
import kitnapper.service.AppContext;
import kitnapper.view.AllLoansScreen;
import kitnapper.view.CatalogScreen;
import kitnapper.view.LoginFrame;
import kitnapper.view.MainFrame;
import kitnapper.view.MyLoansScreen;
import kitnapper.view.Theme;

/**
 * Session and routing wiring (implementation plan §2.4): owns the
 * {@link AppContext}, runs login/logout, builds the main shell for the
 * signed-in role, mounts the screens the role can reach, routes sidebar
 * navigation, and applies the theme toggle. View contracts live in
 * {@link CatalogActions} and {@link LoansActions}; controllers may import
 * views, views never import controllers.
 */
public final class AppController {

    private final AppContext context;
    private final List<Runnable> refreshers = new ArrayList<>();
    private LoginFrame loginFrame;
    private MainFrame mainFrame;
    private CatalogController catalog;
    private BorrowingController borrowing;
    private LoansController loans;
    private CatalogScreen catalogScreen;
    private MyLoansScreen myLoansScreen;
    private AllLoansScreen allLoansScreen;

    public AppController(AppContext context) {
        this.context = context;
    }

    /** Shows the login window; the entry point into the app. */
    public void start() {
        loginFrame = new LoginFrame();
        loginFrame.setOnSignIn(this::attemptLogin);
        loginFrame.setOnThemeToggle(this::toggleTheme);
        loginFrame.setVisible(true);
    }

    /** Runs the mock login; empty return value means success. */
    private Optional<String> attemptLogin(LoginFrame.Credentials credentials) {
        Optional<User> user = context.auth().login(credentials.email(), credentials.password());
        if (user.isEmpty()) {
            return Optional.of("Those credentials don't match an account.");
        }
        context.setCurrentUser(user.get());
        openMain(user.get());
        return Optional.empty();
    }

    private void openMain(User user) {
        catalog = new CatalogController(context);
        borrowing = new BorrowingController(context);
        loans = new LoansController(context);
        mainFrame = new MainFrame(user.getRole(), user.getFullName(), statusLeft(),
            this::navigate, this::signOut, this::toggleTheme);
        // Bound to this session's frame: screens from an earlier session keep
        // their controller listeners until logout, and must not write here.
        MainFrame frame = mainFrame;
        BiConsumer<String, String> status = (left, right) -> {
            if (frame.isDisplayable()) {
                frame.setStatus(left, right);
            }
        };
        refreshers.clear();
        catalogScreen = new CatalogScreen(new CatalogActions(catalog, borrowing, status),
            mainFrame.searchField());
        catalog.setOnChanged(catalogScreen::reload);
        mainFrame.mount("catalog", catalogScreen, catalogScreen.commands(),
            catalogScreen::onShown);
        refreshers.add(catalogScreen::refreshTheme);
        mountLoans(user.getRole(), status);
        mainFrame.showScreen(defaultKey(user.getRole()));
        mainFrame.setVisible(true);
        if (loginFrame != null) {
            loginFrame.dispose();
            loginFrame = null;
        }
    }

    /** Mounts the role's loan screen (§8.3/§8.4); other routes stay placeholders. */
    private void mountLoans(UserRole role, BiConsumer<String, String> status) {
        LoansActions actions = new LoansActions(loans);
        if (role == UserRole.MANAGER) {
            allLoansScreen = new AllLoansScreen(actions, status);
            loans.setOnChanged(allLoansScreen::reload);
            mainFrame.mount("allloans", allLoansScreen, allLoansScreen.commands(),
                allLoansScreen::onShown);
            refreshers.add(allLoansScreen::refreshTheme);
        } else {
            myLoansScreen = new MyLoansScreen(actions, status);
            loans.setOnChanged(myLoansScreen::reload);
            mainFrame.mount("myloans", myLoansScreen, myLoansScreen.commands(),
                myLoansScreen::onShown);
            refreshers.add(myLoansScreen::refreshTheme);
        }
    }

    private void navigate(String key) {
        if (mainFrame != null) {
            mainFrame.setStatus("", "");
            mainFrame.showScreen(key);
        }
    }

    private void signOut() {
        context.setCurrentUser(null);
        if (mainFrame != null) {
            mainFrame.dispose();
            mainFrame = null;
        }
        catalog = null;
        borrowing = null;
        loans = null;
        catalogScreen = null;
        myLoansScreen = null;
        allLoansScreen = null;
        refreshers.clear();
        start();
    }

    /** Session-only theme switch (D3): apply tokens, record state, repaint. */
    private void toggleTheme() {
        boolean dark = !Theme.isDark();
        Theme.apply(dark);
        context.setDarkTheme(dark);
        if (mainFrame != null) {
            mainFrame.refreshTheme();
        }
        for (Runnable refresh : refreshers) {
            refresh.run();
        }
        if (loginFrame != null) {
            loginFrame.refreshTheme();
        }
    }

    private String defaultKey(UserRole role) {
        return role == UserRole.MANAGER ? "dashboard" : "catalog";
    }

    /** Pre-mount status text; the catalog screen replaces it on its first reload. */
    private String statusLeft() {
        return context.dataStore().equipment().size() + " items · 0 selected";
    }
}

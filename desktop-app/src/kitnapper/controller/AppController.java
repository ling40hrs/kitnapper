package kitnapper.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.EquipmentStatus;
import kitnapper.model.User;
import kitnapper.model.UserRole;
import kitnapper.service.AppContext;
import kitnapper.view.CatalogScreen;
import kitnapper.view.LoginFrame;
import kitnapper.view.MainFrame;
import kitnapper.view.Theme;

/**
 * Session and routing wiring (implementation plan §2.4): owns the
 * {@link AppContext}, runs login/logout, builds the main shell for the
 * signed-in role, mounts the catalog screen, routes sidebar navigation, and
 * applies the theme toggle. Controllers may import views; views never import
 * controllers.
 */
public final class AppController {

    private final AppContext context;
    private LoginFrame loginFrame;
    private MainFrame mainFrame;
    private CatalogController catalog;
    private BorrowingController borrowing;
    private CatalogScreen catalogScreen;

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
        mainFrame = new MainFrame(user.getRole(), user.getFullName(), statusLeft(),
            this::navigate, this::signOut, this::toggleTheme);
        catalogScreen = new CatalogScreen(actions(), mainFrame.searchField());
        catalog.setOnChanged(catalogScreen::reload);
        mainFrame.mount("catalog", catalogScreen, catalogScreen.commands(),
            catalogScreen::onShown);
        mainFrame.showScreen(defaultKey(user.getRole()));
        mainFrame.setVisible(true);
        if (loginFrame != null) {
            loginFrame.dispose();
            loginFrame = null;
        }
    }

    /** The §5.7 view contract, backed by the two M3 controllers. */
    private CatalogScreen.Actions actions() {
        return new CatalogScreen.Actions() {
            @Override
            public List<Equipment> search(String query, EquipmentCategory category,
                EquipmentStatus status) {
                catalog.setQuery(query);
                catalog.setCategory(category);
                catalog.setStatus(status);
                return catalog.results();
            }

            @Override
            public boolean isManager() {
                return catalog.isManager();
            }

            @Override
            public boolean canCheckout(Equipment item) {
                return catalog.canCheckout(item);
            }

            @Override
            public String checkoutHint(Equipment item) {
                return catalog.checkoutHint(item);
            }

            @Override
            public List<LocalDate> dueDates() {
                return borrowing.dueDateChoices();
            }

            @Override
            public Optional<String> checkout(Equipment item, LocalDate dueDate) {
                return borrowing.checkout(item, dueDate);
            }

            @Override
            public void setStatus(String left, String right) {
                mainFrame.setStatus(left, right);
            }
        };
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
        catalogScreen = null;
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
        if (catalogScreen != null) {
            catalogScreen.refreshTheme();
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

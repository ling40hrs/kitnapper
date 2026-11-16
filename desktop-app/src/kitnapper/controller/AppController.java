package kitnapper.controller;

import java.util.Optional;
import java.util.function.BiConsumer;

import kitnapper.model.User;
import kitnapper.model.UserRole;
import kitnapper.service.AppContext;
import kitnapper.view.Formats;
import kitnapper.view.LoginFrame;
import kitnapper.view.MainFrame;
import kitnapper.view.Theme;

/**
 * Session and routing wiring (implementation plan §2.4): owns the
 * {@link AppContext}, runs login/logout, builds the main shell for the
 * signed-in role, routes sidebar navigation, and applies the theme toggle.
 * Screen construction and mounting live in {@link ScreenMounts}; view
 * contracts live in {@link CatalogActions} and {@link LoansActions};
 * controllers may import views, views never import controllers.
 */
public final class AppController {

    private final AppContext context;
    private LoginFrame loginFrame;
    private MainFrame mainFrame;
    private ScreenMounts mounts;

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
        mounts = new ScreenMounts(context, frame, status, this::applyTheme);
        mounts.mount(user.getRole());
        // Show first: the screens report their status from onShown, and the
        // status sink ignores frames without a peer (stale sessions), so a
        // pre-show routing would drop the default screen's first status line.
        mainFrame.setVisible(true);
        mainFrame.showScreen(defaultKey(user.getRole()));
        if (loginFrame != null) {
            loginFrame.dispose();
            loginFrame = null;
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
        mounts = null;
        if (mainFrame != null) {
            mainFrame.dispose();
            mainFrame = null;
        }
        start();
    }

    /** Session-only theme switch (D3): the title bar flips the current state. */
    private void toggleTheme() {
        applyTheme(!Theme.isDark());
    }

    /** The one theme path: tokens, session state, and every mounted view. */
    private void applyTheme(boolean dark) {
        Theme.apply(dark);
        context.setDarkTheme(dark);
        if (mainFrame != null) {
            mainFrame.refreshTheme();
        }
        if (mounts != null) {
            mounts.refreshTheme();
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
        return Formats.count(context.dataStore().equipment().size(), "item", "items")
            + " · 0 selected";
    }
}

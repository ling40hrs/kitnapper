package kitnapper.controller;

import java.util.Optional;

import kitnapper.model.User;
import kitnapper.model.UserRole;
import kitnapper.service.AppContext;
import kitnapper.view.LoginFrame;
import kitnapper.view.MainFrame;
import kitnapper.view.Theme;

/**
 * Session and routing wiring (implementation plan §2.4): owns the
 * {@link AppContext}, runs login/logout, builds the main shell for the
 * signed-in role, routes sidebar navigation, and applies the theme
 * toggle. Controllers may import views; views never import controllers.
 */
public final class AppController {

    private final AppContext context;
    private LoginFrame loginFrame;
    private MainFrame mainFrame;

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
        mainFrame.showScreen(defaultKey(user.getRole()));
        mainFrame.setVisible(true);
        if (loginFrame != null) {
            loginFrame.dispose();
            loginFrame = null;
        }
    }

    private void navigate(String key) {
        if (mainFrame != null) {
            mainFrame.showScreen(key);
        }
    }

    private void signOut() {
        context.setCurrentUser(null);
        if (mainFrame != null) {
            mainFrame.dispose();
            mainFrame = null;
        }
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
        if (loginFrame != null) {
            loginFrame.refreshTheme();
        }
    }

    private String defaultKey(UserRole role) {
        return role == UserRole.MANAGER ? "dashboard" : "catalog";
    }

    /** Provisional M2 status text; M3 screens own the real counts. */
    private String statusLeft() {
        return context.dataStore().equipment().size() + " items · 0 selected";
    }
}

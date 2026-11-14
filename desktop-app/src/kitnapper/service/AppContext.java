package kitnapper.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kitnapper.model.LateFeeStrategy;
import kitnapper.model.User;

/**
 * Single holder of the app's service instances (implementation plan §2.2):
 * the {@link DataStore}, the shared {@link LateFeeStrategy}, the auth
 * service, the session user, and the session-only theme state (D3). Views
 * learn about changes through {@link #addChangeListener} (NFR-02, D2).
 * No view imports here — the controller applies the visual theme.
 */
public final class AppContext {

    private final DataStore store;
    private final LateFeeStrategy lateFeeStrategy = new LateFeeStrategy();
    private final AuthService auth;
    private User currentUser;
    private boolean darkTheme = true;
    private final List<Runnable> listeners = new ArrayList<>();

    public AppContext(DataStore store) {
        this.store = Objects.requireNonNull(store, "store");
        this.auth = new AuthServiceImpl(store);
    }

    public DataStore dataStore() {
        return store;
    }

    /** The one fee rule for the whole app (NFR-06); views never compute fees. */
    public LateFeeStrategy lateFeeStrategy() {
        return lateFeeStrategy;
    }

    public AuthService auth() {
        return auth;
    }

    /** Signed-in user, or null before login / after logout. */
    public User currentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        currentUser = user;
        fireChanged();
    }

    /** Session-only theme state (D3); never persisted to the save file. */
    public boolean isDarkTheme() {
        return darkTheme;
    }

    public void setDarkTheme(boolean dark) {
        darkTheme = dark;
        fireChanged();
    }

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    private void fireChanged() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}

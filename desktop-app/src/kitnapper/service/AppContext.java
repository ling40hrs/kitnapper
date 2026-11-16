package kitnapper.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import kitnapper.model.Equipment;
import kitnapper.model.LateFeeStrategy;
import kitnapper.model.User;

/**
 * Single holder of the app's service instances (implementation plan §2.2):
 * the {@link DataStore}, the shared {@link LateFeeStrategy}, the auth
 * service, the catalog, loan and penalty services, the session user, and
 * the session-only theme state (D3). Views learn about changes through
 * {@link #addChangeListener} (NFR-02, D2). No view imports here — the
 * controller applies the visual theme.
 */
public final class AppContext {

    private final DataStore store;
    private final LateFeeStrategy lateFeeStrategy = new LateFeeStrategy();
    private final AuthService auth;
    private final CatalogService catalog;
    private final LoanService loans;
    private final PenaltyService penalties;
    private final ImageStore images;
    private final InventoryService inventory;
    private final Supplier<LocalDate> today;
    private User currentUser;
    private boolean darkTheme = true;
    private final List<Runnable> listeners = new ArrayList<>();

    public AppContext(DataStore store) {
        this(store, LocalDate::now);
    }

    /** Injectable clock so probes and services share one deterministic date. */
    public AppContext(DataStore store, Supplier<LocalDate> today) {
        this.store = Objects.requireNonNull(store, "store");
        this.today = Objects.requireNonNull(today, "today");
        this.auth = new AuthServiceImpl(store);
        this.catalog = new CatalogServiceImpl(store);
        this.loans = new LoanServiceImpl(store, lateFeeStrategy,
                this::currentUser, this::today, this::fireChanged);
        this.penalties = new PenaltyServiceImpl(store, today,
                this::currentUser, this::fireChanged);
        this.images = new ImageStoreImpl(store.folder());
        this.inventory = new InventoryServiceImpl(store, images,
                this::currentUser, this::fireChanged);
        // Load-time orphan sweep (D11): runs after Main's load(), so the
        // equipment list is the authoritative set of keepable photo ids.
        this.images.sweep(store.equipment().stream().map(Equipment::getId).toList());
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

    public CatalogService catalog() {
        return catalog;
    }

    public LoanService loans() {
        return loans;
    }

    /** Penalty reads and settlement (M4-D1). */
    public PenaltyService penalties() {
        return penalties;
    }

    /** Item photos beside the save file (M5, design §12). */
    public ImageStore images() {
        return images;
    }

    /** Inventory mutations for the manager screens (M5, FR-10). */
    public InventoryService inventory() {
        return inventory;
    }

    /** The injected application clock; never null. */
    public LocalDate today() {
        return Objects.requireNonNull(today.get(), "today");
    }

    /** Signed-in user, or null before login / after logout. */
    public User currentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        currentUser = user;
        fireChanged();
    }

    /**
     * Re-binds the session to the freshly seeded account with the same email
     * (D33). The demo reset replaces every user instance, so without this the
     * signed-in manager would keep a {@link User} that is no longer in the
     * store and every role check would read a stale object.
     */
    public void rebindCurrentUser() {
        User current = currentUser;
        if (current == null) {
            return;
        }
        for (User candidate : store.users()) {
            if (candidate.getEmail().equalsIgnoreCase(current.getEmail())) {
                setCurrentUser(candidate);
                return;
            }
        }
        setCurrentUser(null);
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

    /** Services call this after save(); safe with zero listeners. */
    void fireChanged() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}

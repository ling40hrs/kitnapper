package kitnapper.view;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JTable;

import kitnapper.controller.BorrowingController;
import kitnapper.controller.CatalogController;
import kitnapper.model.UserRole;
import kitnapper.service.AppContext;
import kitnapper.service.DataStoreImpl;
import kitnapper.service.persistence.JsonWriter;
import kitnapper.service.persistence.RootMapper;
import kitnapper.service.persistence.SeedData;

/**
 * Catalog UI probe (plan §9.3): mounts the real {@link CatalogScreen} in
 * {@link MainFrame} against a temp-dir seed at {@code T = 2026-11-14}, then
 * drives search, filters, selection, checkout, cards, empty state and theme
 * paints. Production getters stay private: everything is read from the
 * component tree and accessible names. Needs a display (xvfb-run).
 */
public final class CatalogUiProbe {

    private static final LocalDate T = LocalDate.of(2026, 11, 14);

    private CatalogUiProbe() { }

    public static void main(String[] args) throws Exception {
        Theme.apply(true);
        Path dir = Files.createTempDirectory("kitnapper-ui-probe");
        try {
            seed(dir);
            DataStoreImpl store = new DataStoreImpl(dir);
            store.load();
            AppContext ctx = new AppContext(store, () -> T);
            ctx.setCurrentUser(ctx.auth().login("student1@institution.edu", "student123")
                .orElseThrow());
            // Every Swing read and write happens on the EDT: mutating the tree
            // from main while the EDT paints deadlocks on AWTTreeLock.
            javax.swing.SwingUtilities.invokeAndWait(() -> run(ctx, store));
        } finally {
            deleteTree(dir);
        }
        UiProbeSupport.finish();
    }

    private static void run(AppContext ctx, DataStoreImpl store) {
        CatalogController catalog = new CatalogController(ctx);
        BorrowingController borrowing = new BorrowingController(ctx);
        CatalogUiActions actions = new CatalogUiActions(catalog, borrowing);
        MainFrame frame = new MainFrame(UserRole.BORROWER, "Student One", "",
            key -> { }, () -> { }, () -> { });
        CatalogScreen screen = new CatalogScreen(actions, frame.searchField());
        catalog.setOnChanged(screen::reload);
        frame.mount("catalog", screen, screen.commands(), screen::onShown);
        frame.showScreen("catalog");
        frame.setVisible(true);
        UiProbeSupport.settle(frame);

        new CatalogUiChecks(store, actions, screen, frame).run();
        ToastChecks.run();
        FocusCueChecks.run(screen, frame);
        DialogTextChecks.run();
        managerCheck(ctx, actions, frame);
    }

    /** §8.3: managers get the caption, never the checkout button. */
    private static void managerCheck(AppContext ctx, CatalogUiActions actions,
        MainFrame borrowerFrame) {
        ctx.setCurrentUser(ctx.auth().login("admin@institution.edu", "admin123").orElseThrow());
        MainFrame frame = new MainFrame(UserRole.MANAGER, "A. Manager", "",
            key -> { }, () -> { }, () -> { });
        CatalogScreen screen = new CatalogScreen(actions, frame.searchField());
        frame.mount("catalog", screen, screen.commands(), screen::onShown);
        frame.showScreen("catalog");
        frame.setVisible(true);
        UiProbeSupport.settle(frame);
        JTable table = UiProbeSupport.find(screen, JTable.class);
        table.setRowSelectionInterval(0, 0);
        JButton action = UiProbeSupport.button(screen.preview(), "Check out");
        UiProbeSupport.check("manager-readonly", action == null || !action.isVisible());
        UiProbeSupport.check("manager-hint", UiProbeSupport.hasAccessibleName(screen.preview(),
            "Manager view — checkout is borrower-only."));
        frame.dispose();
        borrowerFrame.dispose();
    }

    private static void seed(Path dir) throws java.io.IOException {
        Files.writeString(dir.resolve(DataStoreImpl.SAVE_FILE),
            JsonWriter.write(new RootMapper().toDocument(SeedData.create(T))),
            StandardCharsets.UTF_8);
    }

    private static void deleteTree(Path dir) {
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (java.io.IOException ignored) {
                    // Best effort cleanup of a scratch directory.
                }
            });
        } catch (java.io.IOException ignored) {
            // Best effort cleanup of a scratch directory.
        }
    }
}

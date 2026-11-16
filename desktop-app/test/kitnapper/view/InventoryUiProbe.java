package kitnapper.view;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;

import kitnapper.controller.InventoryActions;
import kitnapper.controller.InventoryController;
import kitnapper.model.UserRole;
import kitnapper.service.AppContext;
import kitnapper.service.DataStoreImpl;
import kitnapper.service.persistence.JsonWriter;
import kitnapper.service.persistence.RootMapper;
import kitnapper.service.persistence.SeedData;

/**
 * Inventory UI probe (M5 WP3, plan §9.4): mounts the real {@link InventoryScreen}
 * in {@link MainFrame} as a manager against a temp-dir seed, then drives the
 * table, the omnibar filter, selection, the preview's metadata and action
 * enablement, the state transitions, and the status counts. Needs a display.
 */
public final class InventoryUiProbe {

    private static final LocalDate T = LocalDate.of(2026, 11, 16);

    private InventoryUiProbe() {
    }

    public static void main(String[] args) throws Exception {
        Theme.apply(true);
        Path dir = Files.createTempDirectory("kitnapper-inventory-ui");
        try {
            seed(dir);
            DataStoreImpl store = new DataStoreImpl(dir);
            store.load();
            AppContext ctx = new AppContext(store, () -> T);
            ctx.setCurrentUser(ctx.auth().login("admin@institution.edu", "admin123")
                .orElseThrow());
            javax.swing.SwingUtilities.invokeAndWait(() -> run(ctx, store));
        } finally {
            deleteTree(dir);
        }
        UiProbeSupport.finish();
    }

    private static void run(AppContext ctx, DataStoreImpl store) {
        InventoryController inventory = new InventoryController(ctx);
        MainFrame frame = new MainFrame(UserRole.MANAGER, "A. Manager", "",
            key -> { }, () -> { }, () -> { });
        InventoryScreen screen = new InventoryScreen(
            new InventoryActions(inventory, new kitnapper.controller.ItemPhotos(ctx),
                (left, right) -> frame.setStatus(left, right)),
            frame.searchField());
        inventory.setOnChanged(screen::reload);
        frame.mount("inventory", screen, null, screen::onShown);
        frame.showScreen("inventory");
        frame.setVisible(true);
        UiProbeSupport.settle(frame);

        new InventoryUiChecks(ctx, store, inventory, screen, frame).run();
        frame.dispose();
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

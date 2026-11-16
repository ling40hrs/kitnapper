package kitnapper.view;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import kitnapper.controller.BorrowingController;
import kitnapper.controller.CatalogController;
import kitnapper.controller.InventoryActions;
import kitnapper.controller.InventoryController;
import kitnapper.controller.ItemPhotos;
import kitnapper.controller.PenaltyController;
import kitnapper.controller.SettingsController;
import kitnapper.model.Penalty;
import kitnapper.model.PenaltyStatus;
import kitnapper.model.PenaltyType;
import kitnapper.model.UserRole;
import kitnapper.service.AppContext;
import kitnapper.service.DataStoreImpl;
import kitnapper.service.persistence.JsonWriter;
import kitnapper.service.persistence.RootMapper;
import kitnapper.service.persistence.SeedData;

/**
 * Geometry probe (plan §8.4, decisions D41–D43): the app stays usable at the
 * minimum window size. Mounts the four real screens at 1024×640, 1280×800,
 * 1366×768 and 1920×1080 and asserts that the preview rows and pinned action
 * stay inside the preview, that tables keep their preferred column widths and
 * scroll instead of clipping, that toasts clear the status bar and the action
 * bar, that Settings fits or scrolls, and that every size paints. The
 * assertions live in {@link GeometryChecks}. Needs a display; runs with motion
 * off like the other display probes.
 */
public final class GeometryProbe {

    private static final LocalDate T = LocalDate.of(2026, 11, 14);
    private static final int[][] SIZES = {
        {1024, 640}, {1280, 800}, {1366, 768}, {1920, 1080}
    };

    private GeometryProbe() { }

    public static void main(String[] args) throws Exception {
        Theme.apply(true);
        Path dir = Files.createTempDirectory("kitnapper-geometry-probe");
        try {
            Files.writeString(dir.resolve(DataStoreImpl.SAVE_FILE),
                JsonWriter.write(new RootMapper().toDocument(SeedData.create(T))),
                StandardCharsets.UTF_8);
            DataStoreImpl store = new DataStoreImpl(dir);
            store.load();
            AppContext ctx = new AppContext(store, () -> T);
            ctx.setCurrentUser(ctx.auth().login("admin@institution.edu", "admin123")
                .orElseThrow());
            // The seed freezes no penalties (SeedData), so the penalties screen
            // would show its empty card; give it two real rows for the layout.
            ctx.dataStore().penalties().add(new Penalty("p-001", "L-003",
                PenaltyType.LATE_FEE, 250.0, T.minusDays(3)));
            ctx.dataStore().penalties().add(new Penalty("p-002", "L-001",
                PenaltyType.LATE_FEE, 150.0, T.minusDays(1)));
            for (int[] size : SIZES) {
                runAt(ctx, size);
            }
        } finally {
            deleteTree(dir);
        }
        UiProbeSupport.finish();
    }

    private static void runAt(AppContext ctx, int[] size) throws Exception {
        mount(size, "catalog", UserRole.BORROWER, frame -> {
            CatalogScreen screen = new CatalogScreen(new CatalogUiActions(
                new CatalogController(ctx), new BorrowingController(ctx)),
                frame.searchField());
            frame.mount("catalog", screen, screen.commands(), screen::onShown);
            return screen;
        });
        mount(size, "inventory", UserRole.MANAGER, frame -> {
            InventoryScreen screen = new InventoryScreen(new InventoryActions(
                new InventoryController(ctx), new ItemPhotos(ctx),
                (left, right) -> frame.setStatus(left, right)), frame.searchField());
            frame.mount("inventory", screen, null, screen::onShown);
            return screen;
        });
        mount(size, "penalties", UserRole.MANAGER, frame -> {
            PenaltiesScreen screen = new PenaltiesScreen(
                new PenaltyController(ctx),
                (left, right) -> frame.setStatus(left, right));
            frame.mount("penalties", screen, null, screen::onShown);
            return screen;
        });
        mount(size, "settings", UserRole.MANAGER, frame -> {
            SettingsScreen screen = new SettingsScreen(
                new SettingsController(ctx, dark -> { }, () -> { }));
            frame.mount("settings", screen, null, screen::onShown);
            return screen;
        });
    }

    /** Builds one screen in a fresh frame, lets layout settle, then asserts. */
    private static void mount(int[] size, String key, UserRole role,
            ScreenBuilder builder) throws Exception {
        MainFrame frame = new MainFrame(role, "Probe", "", ignored -> { }, () -> { },
            () -> { });
        JComponent[] mounted = new JComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            frame.setSize(size[0], size[1]);
            JComponent screen = builder.build(frame);
            mounted[0] = screen;
            frame.showScreen(key);
            frame.setVisible(true);
            UiProbeSupport.settle(frame);
            JTable table = UiProbeSupport.find(screen, JTable.class);
            if (table != null && table.getRowCount() > 0) {
                table.setRowSelectionInterval(0, 0);
            }
            Toasts.show(frame, "Saved " + key);
        });
        settle(frame);
        SwingUtilities.invokeAndWait(() -> {
            JComponent screen = mounted[0];
            String tag = size[0] + "x" + size[1] + "-" + key;
            UiProbeSupport.check(tag + "-paints",
                UiProbeSupport.nonBlank(UiProbeSupport.paint(frame)) > 500);
            GeometryChecks.preview(tag, screen);
            GeometryChecks.table(tag, screen);
            GeometryChecks.toast(tag, frame, screen);
            if ("settings".equals(key)) {
                GeometryChecks.settingsFit(tag, screen);
            }
            if (size[0] == SIZES[0][0]) {
                ThemeSweepChecks.sweep(tag, frame, screen);
            }
            frame.dispose();
        });
    }

    /** Lets queued resize/layout events run, then validates and paints once. */
    private static void settle(MainFrame frame) throws Exception {
        Thread.sleep(120);
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> UiProbeSupport.settle(frame));
        Thread.sleep(60);
        SwingUtilities.invokeAndWait(() -> { });
    }

    private interface ScreenBuilder {
        JComponent build(MainFrame frame);
    }

    private static void deleteTree(Path dir) throws java.io.IOException {
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (java.io.IOException ignored) {
                    // probe temp dir; leak is harmless
                }
            });
        }
    }
}

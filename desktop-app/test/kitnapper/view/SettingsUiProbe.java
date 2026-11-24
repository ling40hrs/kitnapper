package kitnapper.view;

import java.awt.Window;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import kitnapper.controller.SettingsController;
import kitnapper.model.User;
import kitnapper.model.UserRole;
import kitnapper.service.AppContext;
import kitnapper.service.DataStoreImpl;
import kitnapper.service.persistence.JsonWriter;
import kitnapper.service.persistence.RootMapper;
import kitnapper.service.persistence.SeedData;

/**
 * Settings probe (M5 WP5, plan §9.4): the page's sections and switches, the
 * session theme path, the read-only locations, the animate toggle, and the
 * demo reset — baseline restored, photos wiped, session re-bound, screens
 * refreshed. Needs a display.
 */
public final class SettingsUiProbe {

    private static final LocalDate T = LocalDate.of(2026, 11, 16);

    private SettingsUiProbe() {
    }

    public static void main(String[] args) throws Exception {
        Theme.apply(true);
        Path dir = Files.createTempDirectory("kitnapper-settings-probe");
        try {
            Files.writeString(dir.resolve(DataStoreImpl.SAVE_FILE),
                JsonWriter.write(new RootMapper().toDocument(SeedData.create(T))),
                StandardCharsets.UTF_8);
            DataStoreImpl store = new DataStoreImpl(dir);
            store.load();
            AppContext ctx = new AppContext(store, () -> T);
            ctx.setCurrentUser(ctx.auth().login("admin@institution.edu", "admin123")
                .orElseThrow());
            javax.swing.SwingUtilities.invokeAndWait(() -> run(ctx, store, dir));
        } finally {
            deleteTree(dir);
        }
        UiProbeSupport.finish();
    }

    private static void run(AppContext ctx, DataStoreImpl store, Path dir) {
        AtomicInteger resets = new AtomicInteger();
        SettingsController controller = new SettingsController(ctx, dark -> {
            Theme.apply(dark);
            ctx.setDarkTheme(dark);
        }, resets::incrementAndGet);
        MainFrame frame = new MainFrame(UserRole.MANAGER, "A. Manager", "",
            key -> { }, () -> { }, () -> { });
        SettingsScreen screen = new SettingsScreen(controller);
        frame.mount("settings", screen, null, screen::onShown);
        frame.showScreen("settings");
        frame.setVisible(true);
        UiProbeSupport.settle(frame);

        UiProbeSupport.check("the page lists its sections",
            UiProbeSupport.hasLabel(screen, "Appearance")
                && UiProbeSupport.hasLabel(screen, "Storage")
                && UiProbeSupport.hasLabel(screen, "Demo data"));
        UiProbeSupport.check("the dark switch reflects the theme",
            screen.darkSwitch().isOn());
        screen.darkSwitch().toggle();
        UiProbeSupport.check("the dark switch flips the app tokens", !Theme.isDark());
        UiProbeSupport.check("the dark switch records the session state",
            !ctx.isDarkTheme());
        screen.darkSwitch().toggle();
        UiProbeSupport.check("the dark switch flips back", Theme.isDark());

        UiProbeSupport.check("the animate switch reflects the session",
            screen.animateSwitch().isOn());
        screen.animateSwitch().toggle();
        UiProbeSupport.check("the animate switch turns the pulse off",
            !Theme.animateSkeletons);
        screen.animateSwitch().toggle();
        UiProbeSupport.check("the animate switch turns the pulse back on",
            Theme.animateSkeletons);

        UiProbeSupport.check("the save path is shown",
            UiProbeSupport.hasLabel(screen,
                dir.resolve(DataStoreImpl.SAVE_FILE).toString()));
        UiProbeSupport.check("the photo folder is shown",
            UiProbeSupport.hasLabel(screen, ctx.images().folder().toString()));
        UiProbeSupport.check("the reset button is present",
            UiProbeSupport.button(screen, "Reset demo data") != null);

        addPhoto(ctx);
        User before = ctx.currentUser();
        Optional<String> failure = controller.resetDemoData();
        UiProbeSupport.check("the reset succeeds", failure.isEmpty());
        UiProbeSupport.check("the reset restores ten items", store.equipment().size() == 10);
        UiProbeSupport.check("the reset restores three loans", store.loans().size() == 3);
        UiProbeSupport.check("the reset restores the ten seed photos (D50)",
            EditorProbeSupport.jpgCount(ctx.images().folder()) == store.equipment().size());
        UiProbeSupport.check("the session is re-bound", ctx.currentUser() != before
            && ctx.currentUser().getEmail().equals("admin@institution.edu"));
        UiProbeSupport.check("the reset ran the refresh hook", resets.get() == 1);

        ctx.setCurrentUser(store.users().stream()
            .filter(user -> user.getEmail().equals("student1@institution.edu"))
            .findFirst().orElseThrow());
        Optional<String> borrowerFailure = controller.resetDemoData();
        UiProbeSupport.check("a borrower reset is refused with a message",
            borrowerFailure.isPresent()
                && borrowerFailure.get().contains("Only managers"));

        UiProbeSupport.check("the painted page is not blank",
            UiProbeSupport.nonBlank(UiProbeSupport.paint(frame)) > 100);
        frame.dispose();
    }

    private static void addPhoto(AppContext ctx) {
        try {
            Files.createDirectories(ctx.images().folder());
            Files.write(ctx.images().folder().resolve("e-999.jpg"), new byte[] { 1, 2, 3 });
        } catch (java.io.IOException failed) {
            throw new IllegalStateException("could not stage a probe photo", failed);
        }
    }

    private static void deleteTree(Path dir) {
        EditorProbeSupport.deleteTree(dir);
    }
}

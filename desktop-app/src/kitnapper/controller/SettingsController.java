package kitnapper.controller;

import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Consumer;

import kitnapper.service.AppContext;
import kitnapper.service.DataStoreImpl;
import kitnapper.view.PhotoCache;
import kitnapper.view.SettingsScreen;
import kitnapper.view.Theme;

/**
 * The Settings / Data screen's callbacks (M5 WP5, design §5). Theme changes run
 * through the session's {@code applyTheme} so the title-bar toggle and this
 * switch stay one path; the reset wipes photos, reseeds, re-binds the signed-in
 * manager (D33), drops the decoded-photo cache, and refreshes every screen.
 */
public final class SettingsController implements SettingsScreen.Actions {

    private static final String SAVE_FAILED =
            "Could not save the change. Check the app folder and retry.";

    private final AppContext context;
    private final Consumer<Boolean> applyTheme;
    private final Runnable afterReset;

    public SettingsController(AppContext context, Consumer<Boolean> applyTheme,
                              Runnable afterReset) {
        this.context = context;
        this.applyTheme = applyTheme;
        this.afterReset = afterReset;
    }

    @Override
    public boolean isDarkTheme() {
        return Theme.isDark();
    }

    @Override
    public void setDarkTheme(boolean dark) {
        applyTheme.accept(dark);
    }

    @Override
    public String saveFilePath() {
        return context.dataStore().folder().resolve(DataStoreImpl.SAVE_FILE).toString();
    }

    @Override
    public String photoFolderPath() {
        return context.images().folder().toString();
    }

    @Override
    public boolean animateSkeletons() {
        return Theme.animateSkeletons;
    }

    @Override
    public void setAnimateSkeletons(boolean on) {
        Theme.animateSkeletons = on;
    }

    @Override
    public Optional<String> resetDemoData() {
        try {
            context.inventory().resetDemoData();
            context.rebindCurrentUser();
            PhotoCache.clear();
            afterReset.run();
            return Optional.empty();
        } catch (UncheckedIOException saveFailure) {
            return Optional.of(SAVE_FAILED);
        } catch (RuntimeException failure) {
            String message = failure.getMessage();
            return Optional.of(message == null || message.isBlank()
                ? "Could not reset the demo data." : message);
        }
    }
}

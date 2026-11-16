package kitnapper.view;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;

import javax.swing.SwingUtilities;

import kitnapper.model.User;
import kitnapper.service.AppContext;
import kitnapper.service.DataStoreImpl;
import kitnapper.service.persistence.JsonWriter;
import kitnapper.service.persistence.RootMapper;
import kitnapper.service.persistence.SeedData;

/**
 * Manual UI probe for the loan screens (plan §8.3/§8.4/§9.3). Seeds a temp save
 * at {@code T = 2026-11-14}, then runs the borrower pass (My Loans tabs and
 * preview) and the manager pass (All Loans: table, preview, dark/light paint,
 * and the return that freezes the ₱200 penalty, frees {@code e-007} and empties
 * Overdue) before the penalty pane settles it. The check bodies live in
 * {@link LoansUiChecks}; the full §9.3 flow joins the suite at WP6.
 * Needs a display (xvfb-run).
 */
public final class LoansUiProbe {

    private static final LocalDate T = LocalDate.of(2026, 11, 14);

    private LoansUiProbe() { }

    public static void main(String[] args) throws Exception {
        Theme.apply(true);
        Path dir = Files.createTempDirectory("kitnapper-loans-ui-probe");
        try {
            seed(dir);
            DataStoreImpl store = new DataStoreImpl(dir);
            store.load();
            AppContext ctx = new AppContext(store, () -> T);
            // Every Swing read and write happens on the EDT (CatalogUiProbe rule).
            student(ctx);
            SwingUtilities.invokeAndWait(() -> LoansUiChecks.myLoans(ctx));
            manager(ctx);
            SwingUtilities.invokeAndWait(() -> LoansUiChecks.allLoans(ctx, T));
            SwingUtilities.invokeAndWait(() -> LoansUiChecks.penaltyPane(ctx));
        } finally {
            deleteTree(dir);
        }
        UiProbeSupport.finish();
    }

    private static void student(AppContext ctx) {
        ctx.setCurrentUser(login(ctx, "student1@institution.edu", "student123"));
    }

    private static void manager(AppContext ctx) {
        ctx.setCurrentUser(login(ctx, "admin@institution.edu", "admin123"));
    }

    private static User login(AppContext ctx, String email, String password) {
        return ctx.auth().login(email, password).orElseThrow();
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

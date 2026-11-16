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
 * Manual UI probe for the loan and penalty screens (plan §8.3–§8.6/§9.3).
 * Seeds a temp save at {@code T = 2026-11-14} and runs each pass against its
 * own fresh store, because the flows deliberately consume seed state: My Loans
 * (tabs and preview), Overdue (row, preview, and the return that empties the
 * query), All Loans (table, paint, and the return that freezes the ₱200
 * penalty and frees {@code e-007}), then Penalties (settlement and the §8.9
 * body). The check bodies live in {@link LoansUiChecks},
 * {@link OverdueUiChecks} and {@link PenaltyUiChecks}; the full §9.3 flow
 * joins the suite at WP6. Needs a display (xvfb-run).
 */
public final class LoansUiProbe {

    private static final LocalDate T = LocalDate.of(2026, 11, 14);

    private LoansUiProbe() { }

    public static void main(String[] args) throws Exception {
        Theme.apply(true);
        Path dir = Files.createTempDirectory("kitnapper-loans-ui-probe");
        try {
            // Each pass gets its own seeded store: the Overdue pass returns
            // L-003 on purpose, so sharing one store would contradict the
            // All Loans asserts that run after it.
            AppContext ctx = context(dir);
            student(ctx);
            SwingUtilities.invokeAndWait(() -> LoansUiChecks.myLoans(ctx));

            AppContext overdue = context(dir);
            manager(overdue);
            SwingUtilities.invokeAndWait(() -> OverdueUiChecks.overdue(overdue, T));

            AppContext manager = context(dir);
            manager(manager);
            SwingUtilities.invokeAndWait(() -> LoansUiChecks.allLoans(manager, T));
            SwingUtilities.invokeAndWait(() -> PenaltyUiChecks.penalties(manager));
            SwingUtilities.invokeAndWait(() -> PenaltyUiChecks.dialogBody(manager));
        } finally {
            deleteTree(dir);
        }
        UiProbeSupport.finish();
    }

    /** A fresh store over a freshly seeded save file (probe isolation). */
    private static AppContext context(Path dir) throws java.io.IOException {
        seed(dir);
        DataStoreImpl store = new DataStoreImpl(dir);
        store.load();
        return new AppContext(store, () -> T);
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

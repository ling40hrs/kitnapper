package kitnapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import kitnapper.model.Equipment;
import kitnapper.model.EquipmentStatus;
import kitnapper.model.LoanRecord;
import kitnapper.model.User;
import kitnapper.service.DataStore;

/**
 * Shared probe plumbing (WP0.5): the named-check tally every probe prints,
 * exception guards for invariant checks, seed lookups, and scratch-dir
 * cleanup. Test-only; nothing under test/ is compiled into the app jar.
 */
final class ProbeSupport {

    private static int passed;
    private static int failed;

    private ProbeSupport() {
    }

    static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }

    /** The one summary line a probe prints before exiting. */
    static String tally() {
        return "PASS=" + passed + " FAIL=" + failed;
    }

    static boolean failing() {
        return failed > 0;
    }

    static boolean throwsISE(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalStateException expected) {
            return true;
        }
    }

    static boolean throwsIAE(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    /** The loan with the given ID, or null when absent. */
    static LoanRecord loan(DataStore store, String id) {
        return store.loans().stream()
                .filter(l -> l.getLoanId().equals(id))
                .findFirst().orElse(null);
    }

    /** The equipment item with the given ID, or null when absent. */
    static Equipment item(DataStore store, String id) {
        return store.equipment().stream()
                .filter(i -> i.getId().equals(id))
                .findFirst().orElse(null);
    }

    /** True when the item exists and is LOANED. */
    static boolean loanedItem(DataStore store, String id) {
        Equipment item = item(store, id);
        return item != null && item.getStatus() == EquipmentStatus.LOANED;
    }

    /** The account with the given email (case-insensitive), or null. */
    static User user(DataStore store, String email) {
        return store.users().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst().orElse(null);
    }

    static void deleteTree(Path dir) {
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // Best effort cleanup of a scratch directory.
                }
            });
        } catch (IOException ignored) {
            // Best effort cleanup of a scratch directory.
        }
    }
}

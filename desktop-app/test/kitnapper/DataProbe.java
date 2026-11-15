package kitnapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import kitnapper.model.EquipmentStatus;
import kitnapper.model.UserRole;
import kitnapper.service.AppContext;
import kitnapper.service.DataStore;
import kitnapper.service.DataStoreImpl;

/**
 * Seed probe (plan §6.4, §9.4): seed counts and shapes against a fresh
 * scratch directory. The invariant and persistence sections live in
 * {@link DataInvariantChecks}; both print through the shared tally and the
 * probe exits non-zero on any failure. Runs headless.
 */
public final class DataProbe {

    private DataProbe() { }

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("kitnapper-data-probe");
        try {
            DataStore store = new DataStoreImpl(dir);
            store.load();
            LocalDate t = ProbeSupport.loan(store, "L-003").getDueDate().plusDays(4);
            seedChecks(store, t);
            DataInvariantChecks.run(t);
        } finally {
            ProbeSupport.deleteTree(dir);
        }
        System.out.println(ProbeSupport.tally());
        if (ProbeSupport.failing()) {
            System.exit(1);
        }
    }

    private static void seedChecks(DataStore store, LocalDate t) {
        long managers = store.users().stream()
                .filter(u -> u.getRole() == UserRole.MANAGER).count();
        ProbeSupport.check("seed-users", store.users().size() == 3 && managers == 1);
        ProbeSupport.check("seed-equipment", store.equipment().size() == 10
            && ProbeSupport.item(store, "e-001") != null
            && ProbeSupport.item(store, "e-010") != null);
        ProbeSupport.check("seed-loans", store.loans().size() == 3
            && store.penalties().isEmpty()
            && ProbeSupport.loan(store, "L-001") != null
            && ProbeSupport.loan(store, "L-003") != null);
        long available = store.equipment().stream()
                .filter(i -> i.getStatus() == EquipmentStatus.AVAILABLE).count();
        ProbeSupport.check("seed-status-mix", available == 7
            && ProbeSupport.loanedItem(store, "e-001")
            && ProbeSupport.loanedItem(store, "e-005")
            && ProbeSupport.loanedItem(store, "e-007"));
        ProbeSupport.check("seed-loan-shape",
            ProbeSupport.loan(store, "L-003").getBorrowerId().equals("u-student1")
            && ProbeSupport.loan(store, "L-003").getEquipmentId().equals("e-007")
            && ProbeSupport.loan(store, "L-003").getDueDate().equals(t.minusDays(4))
            && ProbeSupport.loan(store, "L-003").getLoanStatus()
                == kitnapper.model.LoanStatus.ACTIVE);
        ProbeSupport.check("seed-derived-overdue",
            ProbeSupport.loan(store, "L-003").effectiveStatus(t)
                == kitnapper.model.LoanStatus.OVERDUE
            && ProbeSupport.loan(store, "L-001").effectiveStatus(t)
                == kitnapper.model.LoanStatus.ACTIVE);
        ProbeSupport.check("seed-persisted-never-overdue", store.loans().stream()
            .allMatch(l -> l.getLoanStatus() != kitnapper.model.LoanStatus.OVERDUE));
        AppContext context = new AppContext(store, () -> t);
        ProbeSupport.check("seed-overdue-query",
            context.loans().overdueLoans().size() == 1
            && context.loans().overdueLoans().get(0).getLoanId().equals("L-003"));
        ProbeSupport.check("seed-fees",
            context.loans().accruedFee("L-003") == 200.0
            && context.loans().accruedFee("L-001") == 0.0
            && context.loans().accruedFee("L-002") == 0.0
            && kitnapper.model.LateFeeStrategy.RATE_PER_DAY == 50.0);
        double total = 0;
        for (kitnapper.model.Equipment item : store.equipment()) {
            total += item.getReplacementValue();
        }
        ProbeSupport.check("seed-value", total == 872000.0);
    }
}

package kitnapper;

import java.nio.file.Path;
import java.time.LocalDate;

import kitnapper.controller.LoansController;
import kitnapper.controller.PenaltyController;
import kitnapper.model.EquipmentStatus;
import kitnapper.model.LoanStatus;
import kitnapper.model.PenaltyOutcome;
import kitnapper.model.PenaltyStatus;
import kitnapper.service.AppContext;
import kitnapper.service.DataStore;
import kitnapper.service.DataStoreImpl;
import kitnapper.view.LoanRow;
import kitnapper.view.PenaltyRow;

/**
 * Penalty-side checks for {@link LoanProbe}: a second frozen fee via the
 * advanced clock (D20), manager-only one-way settlement (M4-D31), and
 * persistence across a fresh load.
 */
final class PenaltyChecks {

    private static final String ADMIN = "admin@institution.edu";
    private static final String STUDENT1 = "student1@institution.edu";
    private static final String STUDENT2 = "student2@institution.edu";

    private PenaltyChecks() {
    }

    /** Clock is T+3 now: L-002 is 2 days overdue, so the return freezes ₱100. */
    static void secondPenalty(LoansController loans, AppContext context,
                              DataStore store, LocalDate t) {
        ProbeSupport.signIn(context, store, ADMIN);
        LoanRow l002 = loans.allLoans().stream()
                .filter(row -> row.loanId().equals("L-002"))
                .findFirst().orElseThrow();
        ProbeSupport.check("second-penalty",
                loans.returnLoan(l002, t.plusDays(3)).isEmpty()
                && ProbeSupport.loan(store, "L-002").getLoanStatus()
                        == LoanStatus.RETURNED
                && ProbeSupport.item(store, "e-005").getStatus()
                        == EquipmentStatus.AVAILABLE
                && store.penalties().size() == 2
                && store.penalties().get(1).getPenaltyId().equals("p-002")
                && store.penalties().get(1).getAmount() == 100.0
                && store.penalties().get(1).getStatus() == PenaltyStatus.UNPAID
                && store.penalties().get(1).getAssessedDate().equals(t.plusDays(3)));
    }

    /** Settlement rules: unknown id, borrower-only, blank reason, one-way. */
    static void settlement(PenaltyController penalties, AppContext context,
                           DataStore store, LocalDate t) {
        ProbeSupport.signIn(context, store, ADMIN);
        PenaltyRow ghost = new PenaltyRow("p-999", "L-999", "Ghost item",
                "Nobody", 1.0, PenaltyStatus.UNPAID, t, null);
        ProbeSupport.check("settle-unknown-rejected",
                penalties.settle(ghost, PenaltyOutcome.PAID, null).isPresent()
                && store.penalties().size() == 2);
        ProbeSupport.signIn(context, store, STUDENT1);
        ProbeSupport.check("settle-borrower-rejected",
                penalties.settle(rowOf(penalties, "p-001"), PenaltyOutcome.PAID,
                        null).isPresent()
                && store.penalties().get(0).getStatus() == PenaltyStatus.UNPAID);
        ProbeSupport.signIn(context, store, ADMIN);
        ProbeSupport.check("waive-blank-rejected",
                penalties.settle(rowOf(penalties, "p-001"), PenaltyOutcome.WAIVED,
                        "   ").isPresent()
                && store.penalties().get(0).getStatus() == PenaltyStatus.UNPAID);
        ProbeSupport.check("mark-paid",
                penalties.settle(rowOf(penalties, "p-001"), PenaltyOutcome.PAID,
                        null).isEmpty()
                && store.penalties().get(0).getStatus() == PenaltyStatus.PAID
                && context.penalties().unpaidTotal() == 100.0);
        ProbeSupport.check("re-settle-rejected",
                penalties.settle(rowOf(penalties, "p-001"), PenaltyOutcome.WAIVED,
                        "late excuse").isPresent()
                && penalties.settle(rowOf(penalties, "p-001"), PenaltyOutcome.PAID,
                        null).isPresent()
                && store.penalties().get(0).getStatus() == PenaltyStatus.PAID);
        ProbeSupport.check("waive-reason",
                penalties.settle(rowOf(penalties, "p-002"), PenaltyOutcome.WAIVED,
                        "First offense").isEmpty()
                && store.penalties().get(1).getStatus() == PenaltyStatus.WAIVED
                && store.penalties().get(1).getWaiveReason().orElse("")
                        .equals("First offense")
                && context.penalties().unpaidTotal() == 0.0);
        ProbeSupport.signIn(context, store, STUDENT2);
        ProbeSupport.check("my-penalties-student2",
                context.penalties().myPenalties().size() == 1
                && context.penalties().myPenalties().get(0).getPenaltyId()
                        .equals("p-002"));
    }

    /** A fresh load must show every return and settlement decision. */
    static void persistence(DataStore store, Path dir) {
        DataStore reloaded = new DataStoreImpl(dir);
        reloaded.load();
        ProbeSupport.check("persist-reload",
                ProbeSupport.loan(reloaded, "L-003").getLoanStatus()
                        == LoanStatus.RETURNED
                && reloaded.penalties().size() == 2
                && reloaded.penalties().get(0).getStatus() == PenaltyStatus.PAID
                && reloaded.penalties().get(1).getStatus() == PenaltyStatus.WAIVED
                && reloaded.penalties().get(1).getWaiveReason().orElse("")
                        .equals("First offense")
                && ProbeSupport.item(reloaded, "e-007").getStatus()
                        == EquipmentStatus.AVAILABLE);
    }

    private static PenaltyRow rowOf(PenaltyController penalties, String id) {
        return penalties.penalties().stream()
                .filter(row -> row.penaltyId().equals(id))
                .findFirst()
                .orElseThrow();
    }
}

package kitnapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import kitnapper.controller.BorrowingController;
import kitnapper.model.Borrower;
import kitnapper.model.Equipment;
import kitnapper.model.EquipmentStatus;
import kitnapper.model.LoanStatus;
import kitnapper.model.Manager;
import kitnapper.model.PenaltyType;
import kitnapper.service.AppContext;
import kitnapper.service.DataStoreImpl;
import kitnapper.service.persistence.JsonWriter;
import kitnapper.service.persistence.RootMapper;
import kitnapper.service.persistence.SeedData;

/**
 * Checkout/return rules probe (plan §9.2 items 9–28): the checkout and
 * return rule set (caps, window, availability, roles), fee freezing, and
 * the D15 maintenance rule — against a deterministic clock T = 2026-11-14
 * and a temp-dir save file. The search section lives in
 * {@link CatalogSearchChecks}. Runs headless; exits non-zero on failure.
 */
public final class CatalogProbe {

    private static final LocalDate T = LocalDate.of(2026, 11, 14);

    private CatalogProbe() { }

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("kitnapper-catalog-probe");
        try {
            Files.writeString(dir.resolve(DataStoreImpl.SAVE_FILE),
                JsonWriter.write(new RootMapper().toDocument(SeedData.create(T))),
                StandardCharsets.UTF_8);
            DataStoreImpl store = new DataStoreImpl(dir);
            store.load();
            run(new AppContext(store, () -> T), store, dir);
        } finally {
            ProbeSupport.deleteTree(dir);
        }
        System.out.println(ProbeSupport.tally());
        if (ProbeSupport.failing()) {
            System.exit(1);
        }
    }

    private static void run(AppContext ctx, DataStoreImpl store, Path dir) {
        CatalogSearchChecks.run(ctx.catalog());
        Borrower s1 = (Borrower) ProbeSupport.user(store, "student1@institution.edu");
        Borrower s2 = (Borrower) ProbeSupport.user(store, "student2@institution.edu");
        Manager admin = (Manager) ProbeSupport.user(store, "admin@institution.edu");
        BorrowingController borrowing = new BorrowingController(ctx);

        ctx.setCurrentUser(s1);
        Equipment canon = ProbeSupport.item(store, "e-002");
        kitnapper.model.LoanRecord loan =
            ctx.loans().checkout(canon.getId(), T.plusDays(3));
        ProbeSupport.check("checkout-success", loan.getLoanId().equals("L-004")
            && canon.getStatus() == EquipmentStatus.LOANED && store.loans().size() == 4);
        DataStoreImpl reloaded = new DataStoreImpl(dir);
        reloaded.load();
        ProbeSupport.check("checkout-persists", reloaded.loans().size() == 4
            && ProbeSupport.item(reloaded, "e-002").getStatus() == EquipmentStatus.LOANED
            && reloaded.loans().stream()
                .filter(l -> l.getLoanId().equals("L-004")).findFirst().orElseThrow()
                .getDueDate().equals(T.plusDays(3)));
        ProbeSupport.check("cap-rejects",
            ProbeSupport.throwsISE(() -> ctx.loans().checkout("e-003", T.plusDays(3)))
            && ProbeSupport.item(store, "e-003").getStatus() == EquipmentStatus.AVAILABLE
            && store.loans().size() == 4);

        ctx.setCurrentUser(s2);
        ProbeSupport.check("window-rejects",
            ProbeSupport.throwsIAE(() -> ctx.loans().checkout("e-003", T.plusDays(15)))
            && ProbeSupport.item(store, "e-003").getStatus() == EquipmentStatus.AVAILABLE);
        ProbeSupport.check("unavailable-rejects",
            ProbeSupport.throwsISE(() -> ctx.loans().checkout("e-001", T.plusDays(3))));
        ctx.setCurrentUser(admin);
        ProbeSupport.check("manager-checkout-rejects",
            ProbeSupport.throwsISE(() -> ctx.loans().checkout("e-003", T.plusDays(3))));
        ctx.setCurrentUser(s2);
        ProbeSupport.check("past-due-rejects",
            ProbeSupport.throwsIAE(() -> ctx.loans().checkout("e-003", T.minusDays(1))));

        ProbeSupport.check("return-manager-only",
            ProbeSupport.throwsISE(() -> ctx.loans().returnLoan("L-003", T)));
        ctx.setCurrentUser(admin);
        ctx.loans().returnLoan("L-003", T);
        ProbeSupport.check("return-freezes-fee",
            ProbeSupport.item(store, "e-007").getStatus() == EquipmentStatus.AVAILABLE
            && store.penalties().size() == 1
            && store.penalties().get(0).getPenaltyId().equals("p-001")
            && store.penalties().get(0).getType() == PenaltyType.LATE_FEE
            && store.penalties().get(0).getAmount() == 200
            && loanStatus(store, "L-003") == LoanStatus.RETURNED);
        ProbeSupport.check("double-return-rejects",
            ProbeSupport.throwsISE(() -> ctx.loans().returnLoan("L-003", T)));
        ProbeSupport.check("future-return-rejects",
            ProbeSupport.throwsIAE(() -> ctx.loans().returnLoan("L-002", T.plusDays(1)))
            && loanStatus(store, "L-002") == LoanStatus.ACTIVE);

        ctx.setCurrentUser(s2);
        ctx.loans().checkout("e-003", T);
        ctx.setCurrentUser(admin);
        ctx.loans().returnLoan("L-005", T);
        ProbeSupport.check("zero-fee-no-penalty", store.penalties().size() == 1
            && ProbeSupport.item(store, "e-003").getStatus() == EquipmentStatus.AVAILABLE
            && loanStatus(store, "L-005") == LoanStatus.RETURNED);

        ProbeSupport.item(store, "e-001").markUnderMaintenance();
        ctx.loans().returnLoan("L-001", T);
        ProbeSupport.check("maintenance-survives-return",
            ProbeSupport.item(store, "e-001").getStatus()
                == EquipmentStatus.UNDER_MAINTENANCE
            && loanStatus(store, "L-001") == LoanStatus.RETURNED);
        ProbeSupport.check("fee-frozen-after-return", ctx.loans().accruedFee("L-003") == 200);
        ProbeSupport.check("overdue-empty-after", ctx.loans().overdueLoans().isEmpty());

        ctx.setCurrentUser(s1);
        ProbeSupport.check("myloans-filter", ctx.loans().myLoans().size() == 3
            && ctx.loans().myLoans().stream()
                .allMatch(l -> l.getBorrowerId().equals("u-student1")));
        ProbeSupport.check("allloans-include-all", ctx.loans().allLoans().size() == 5
            && ctx.loans().allLoans().stream()
                .anyMatch(l -> l.getBorrowerId().equals("u-student2")));
        ProbeSupport.check("due-date-choices", borrowing.dueDateChoices().size() == 15
            && borrowing.dueDateChoices().get(0).equals(T)
            && borrowing.dueDateChoices().get(14).equals(T.plusDays(14)));

        ctx.setCurrentUser(s2);
        Optional<String> ok = borrowing.checkout(
            ProbeSupport.item(store, "e-004"), T.plusDays(2));
        ProbeSupport.check("controller-checkout-success", ok.isEmpty()
            && ProbeSupport.item(store, "e-004").getStatus() == EquipmentStatus.LOANED);
        ctx.setCurrentUser(s1);
        Optional<String> rejected = borrowing.checkout(
            ProbeSupport.item(store, "e-006"), T.plusDays(15));
        ProbeSupport.check("controller-checkout-failure", rejected.isPresent()
            && !rejected.orElse("").isBlank());
    }

    private static LoanStatus loanStatus(DataStoreImpl store, String loanId) {
        return store.loans().stream()
                .filter(l -> l.getLoanId().equals(loanId))
                .findFirst().orElseThrow()
                .getLoanStatus();
    }
}

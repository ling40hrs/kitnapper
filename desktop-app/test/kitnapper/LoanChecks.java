package kitnapper;

import java.time.LocalDate;
import java.util.List;

import kitnapper.controller.LoansController;
import kitnapper.model.EquipmentStatus;
import kitnapper.model.LoanRecord;
import kitnapper.model.LoanStatus;
import kitnapper.model.PenaltyStatus;
import kitnapper.service.AppContext;
import kitnapper.service.DataStore;
import kitnapper.view.LoanRow;

/**
 * Loan-side checks for {@link LoanProbe}: seed reads, the accruedFee
 * overload (M4-D7), controller rows and the return workflow (plan §9.2
 * items 1–11). Session switching is explicit in each phase.
 */
final class LoanChecks {

    private static final String ADMIN = "admin@institution.edu";
    private static final String STUDENT1 = "student1@institution.edu";
    private static final String STUDENT2 = "student2@institution.edu";

    private LoanChecks() {
    }

    /** Seed counts, scoped reads and the fee overload, before any return. */
    static void seedReads(AppContext context, DataStore store, LocalDate t) {
        ProbeSupport.signIn(context, store, ADMIN);
        List<LoanRecord> all = context.loans().allLoans();
        ProbeSupport.check("seed-all-loans", all.size() == 3
                && all.get(0).getLoanId().equals("L-001")
                && all.get(1).getLoanId().equals("L-002")
                && all.get(2).getLoanId().equals("L-003"));
        ProbeSupport.signIn(context, store, STUDENT1);
        int mine = context.loans().myLoans().size();
        ProbeSupport.signIn(context, store, STUDENT2);
        ProbeSupport.check("seed-my-loans", mine == 2
                && context.loans().myLoans().size() == 1);
        ProbeSupport.signIn(context, store, ADMIN);
        ProbeSupport.check("seed-overdue",
                context.loans().overdueLoans().size() == 1
                && context.loans().overdueLoans().get(0).getLoanId().equals("L-003"));
        ProbeSupport.check("seed-overdue-metrics",
                context.loans().accruedFee("L-003") == 200.0
                && ProbeSupport.loan(store, "L-003").daysOverdue(t) == 4);
        ProbeSupport.signIn(context, store, STUDENT1);
        ProbeSupport.check("seed-penalties",
                context.penalties().allPenalties().isEmpty()
                && context.penalties().unpaidTotal() == 0.0
                && context.penalties().myPenalties().isEmpty()
                && context.penalties().penaltiesForLoan("L-003").isEmpty()
                && context.penalties().penaltiesForLoan("L-999").isEmpty()
                && context.penalties().penaltiesForLoan(" ").isEmpty());
        ProbeSupport.check("fee-overload",
                context.loans().accruedFee("L-003", t.minusDays(1)) == 150.0
                && context.loans().accruedFee("L-003", t) == 200.0
                && context.loans().accruedFee("L-003", t.minusDays(4)) == 0.0
                && context.loans().accruedFee("L-002", t) == 0.0);
    }

    /** My Loans tabs and resolved row fields, still on the seed state. */
    static void controllerRows(LoansController loans, AppContext context,
                               LocalDate t) {
        ProbeSupport.signIn(context, store(context), STUDENT1);
        ProbeSupport.check("rows-my-tabs",
                loans.myLoans(LoanStatus.OVERDUE).size() == 1
                && loans.myLoans(LoanStatus.ACTIVE).size() == 1
                && loans.myLoans(LoanStatus.RETURNED).isEmpty());
        LoanRow overdue = loans.myLoans(LoanStatus.OVERDUE).get(0);
        ProbeSupport.check("rows-my-overdue-row",
                overdue.loanId().equals("L-003")
                && overdue.itemName().equals("Rode Wireless PRO kit")
                && overdue.borrowerName().equals("Student One")
                && overdue.accruedFee() == 200.0
                && overdue.daysLate() == 4
                && overdue.penalty() == null
                && overdue.returnDate() == null);
        List<LoanRow> all = loans.allLoans();
        ProbeSupport.check("rows-all-order", all.size() == 3
                && all.get(0).loanId().equals("L-001")
                && all.get(1).loanId().equals("L-002")
                && all.get(2).loanId().equals("L-003")
                && all.stream().allMatch(row -> !row.itemName().isBlank()
                        && !row.borrowerName().isBlank()));
        ProbeSupport.check("rows-overdue", loans.overdueLoans().size() == 1
                && loans.overdueLoans().get(0).loanId().equals("L-003")
                && loans.overdueLoans().get(0).daysLate() == 4
                && loans.overdueLoans().get(0).accruedFee() == 200.0);
        List<LocalDate> dates = loans.returnDates(overdue);
        ProbeSupport.check("rows-return-dates", dates.size() == 12
                && dates.get(0).equals(t.minusDays(11))
                && dates.get(dates.size() - 1).equals(t));
    }

    /**
     * The return workflow: borrower rejected, L-003 frozen into one
     * penalty, repeat/future/pre-borrow rejected, D15 maintenance survives,
     * zero fee freezes nothing.
     */
    static void returns(LoansController loans,
                        kitnapper.controller.DashboardController dashboard,
                        AppContext context, DataStore store, LocalDate t) {
        LoanRow overdue = loans.overdueLoans().get(0);
        ProbeSupport.signIn(context, store, STUDENT2);
        ProbeSupport.check("return-borrower-rejected",
                loans.returnLoan(overdue, t).isPresent()
                && ProbeSupport.loan(store, "L-003").getLoanStatus()
                        != LoanStatus.RETURNED);
        ProbeSupport.signIn(context, store, ADMIN);
        ProbeSupport.check("return-l003", loans.returnLoan(overdue, t).isEmpty()
                && ProbeSupport.loan(store, "L-003").getLoanStatus()
                        == LoanStatus.RETURNED
                && ProbeSupport.item(store, "e-007").getStatus()
                        == EquipmentStatus.AVAILABLE
                && store.penalties().size() == 1
                && store.penalties().get(0).getPenaltyId().equals("p-001")
                && store.penalties().get(0).getAmount() == 200.0
                && store.penalties().get(0).getStatus() == PenaltyStatus.UNPAID
                && store.penalties().get(0).getAssessedDate().equals(t));
        ProbeSupport.check("dashboard-mid", dashboard.loansOut() == 2
                && dashboard.overdueCount() == 0
                && dashboard.unpaidCount() == 1
                && dashboard.unpaidTotal().equals("₱200.00"));
        ProbeSupport.check("return-repeat-rejected",
                loans.returnLoan(overdue, t).isPresent()
                && store.penalties().size() == 1);
        LoanRow l001 = loans.allLoans().stream()
                .filter(row -> row.loanId().equals("L-001"))
                .findFirst().orElseThrow();
        ProbeSupport.check("return-future-rejected",
                loans.returnLoan(l001, t.plusDays(1)).isPresent()
                && ProbeSupport.loan(store, "L-001").getLoanStatus()
                        == LoanStatus.ACTIVE);
        ProbeSupport.check("return-before-borrow-rejected",
                loans.returnLoan(l001, t.minusDays(3)).isPresent());
        ProbeSupport.item(store, "e-001").markUnderMaintenance();
        ProbeSupport.check("return-maintenance-zero-fee",
                loans.returnLoan(l001, t).isEmpty()
                && ProbeSupport.loan(store, "L-001").getLoanStatus()
                        == LoanStatus.RETURNED
                && ProbeSupport.item(store, "e-001").getStatus()
                        == EquipmentStatus.UNDER_MAINTENANCE
                && store.penalties().size() == 1);
        ProbeSupport.check("return-fee-frozen",
                context.loans().accruedFee("L-003") == 200.0);
    }

    private static DataStore store(AppContext context) {
        return context.dataStore();
    }
}
